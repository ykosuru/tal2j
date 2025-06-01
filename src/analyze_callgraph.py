#!/usr/bin/env python3
"""
TAL Procedure Call Graph Generator
Analyzes TAL source files (.TXT) and generates procedure call graphs
Generates DOT, Mermaid, and HTML visualizations, highlighting leaf nodes
"""

import os
import re
import json
import argparse
from pathlib import Path
from typing import Dict, List, Set
from collections import defaultdict
from dataclasses import dataclass

@dataclass
class ProcedureInfo:
    """Information about a procedure"""
    name: str
    file_path: str
    line_number: int
    calls: List[str]
    called_by: List[str]
    is_external: bool = False
    is_leaf: bool = False

class TALCallGraphAnalyzer:
    """Analyzes TAL source files and generates call graphs"""
    
    def __init__(self):
        # Pattern to match procedure definitions
        self.proc_def_pattern = re.compile(
            r'^\s*(?:(int|string|real|fixed)\s+)?(proc|subproc)\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*(?:\([^)]*\))?\s*;?\s*$',
            re.IGNORECASE | re.MULTILINE
        )
        
        # Pattern to match procedure calls
        self.call_pattern = re.compile(
            r'\bcall\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*(?:\([^)]*\))?\s*;?',
            re.IGNORECASE
        )
        
        # Pattern to match function calls in assignments
        self.function_call_pattern = re.compile(
            r'(?:=|:=)\s*([a-zA-Z^][a-zA-Z0-9^_]*)\s*\(',
            re.IGNORECASE
        )
        
        # Pattern to match direct procedure calls in expressions
        self.expression_call_pattern = re.compile(
            r'\b([a-zA-Z^][a-zA-Z0-9^_]*)\s*\([^)]*\)',
            re.IGNORECASE
        )
        
        # Pattern to match procedure references in control structures
        self.procedure_reference_pattern = re.compile(
            r'\b(if|while|case)\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*\(',
            re.IGNORECASE
        )
        
        # External system patterns
        self.external_patterns = {
            'TANDEM': re.compile(r'\bTANDEM_[A-Z0-9_^]+', re.IGNORECASE),
            'IFT3TAL': re.compile(r'\bIFT3TAL_[A-Z0-9_^]+', re.IGNORECASE),
            'IFT3DDLS': re.compile(r'\bIFT3DDLS_[A-Z0-9_^]+', re.IGNORECASE),
            'NSCRIBE': re.compile(r'\b(?:N|EN)SCRIBE[A-Z0-9_^]*', re.IGNORECASE),
            'GUARDIAN': re.compile(r'\bGUARDIAN[A-Z0-9_^]*', re.IGNORECASE),
            'PATHWAY': re.compile(r'\bPATHWAY[A-Z0-9_^]*', re.IGNORECASE),
            'SQL': re.compile(r'\b(EXEC\s+SQL|SELECT|INSERT|UPDATE|DELETE)\b', re.IGNORECASE)
        }
        
        # Built-in functions to exclude
        self.builtin_functions = {
            'LEN', 'SIZE', '$LEN', '$SIZE', '$OCCURS', '$OFFSET', '$TYPE', '$NUMERIC',
            'CONVERTTIMESTAMP', 'JULIANTIMESTAMP', 'INTERPRETTIMESTAMP',
            'MOVL', 'MOVR', 'SCAN', 'RSCAN', 'FILL', 'COMP',
            'OPEN', 'CLOSE', 'READ', 'WRITE', 'POSITION', 'CONTROL',
            'FILEINFO', 'FILENAME', 'SETMODE', 'GETMODE',
            'IF', 'WHILE', 'FOR', 'CASE', 'RETURN', 'BEGIN', 'END'
        }
        
        self.procedures = {}  # procedure_name -> ProcedureInfo
        self.call_graph = defaultdict(set)  # caller -> set of callees
        self.reverse_graph = defaultdict(set)  # callee -> set of callers
        self.external_calls = defaultdict(set)  # procedure -> external systems
    
    def clean_identifier(self, identifier: str) -> str:
        """Clean TAL identifier (replace ^ with _)"""
        return identifier.replace('^', '_') if identifier else identifier
    
    def find_tal_files(self, directory: str) -> List[str]:
        """Find all .TXT files (TAL source) in directory"""
        tal_files = []
        for root, _, files in os.walk(directory):
            for file in files:
                if file.upper().endswith('.TXT'):
                    tal_files.append(os.path.join(root, file))
        return sorted(tal_files)
    
    def analyze_file(self, file_path: str) -> Dict[str, ProcedureInfo]:
        """Analyze a single TAL file for procedures and calls"""
        try:
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
        except Exception as e:
            print(f"Error reading {file_path}: {e}")
            return {}
        
        file_procedures = {}
        
        # Find procedure definitions
        for match in self.proc_def_pattern.finditer(content):
            proc_name = self.clean_identifier(match.group(3))
            line_number = content[:match.start()].count('\n') + 1
            
            # Extract procedure body
            proc_start = match.end()
            next_proc = self.proc_def_pattern.search(content, proc_start)
            proc_end = next_proc.start() if next_proc else len(content)
            proc_body = content[proc_start:proc_end]
            
            # Find calls within this procedure
            calls = self.find_calls_in_procedure(proc_body)
            
            # Check for external system calls
            external_calls = self.find_external_calls(proc_body)
            
            proc_info = ProcedureInfo(
                name=proc_name,
                file_path=file_path,
                line_number=line_number,
                calls=calls,
                called_by=[],
                is_external=len(external_calls) > 0,
                is_leaf=False  # Will be updated in build_call_graph
            )
            
            file_procedures[proc_name] = proc_info
            
            # Store external calls
            if external_calls:
                self.external_calls[proc_name] = external_calls
        
        return file_procedures
    
    def find_calls_in_procedure(self, proc_body: str) -> List[str]:
        """Find all procedure calls within a procedure body"""
        calls = set()
        
        # Find explicit CALL statements
        for match in self.call_pattern.finditer(proc_body):
            called_proc = self.clean_identifier(match.group(1))
            if called_proc.upper() not in self.builtin_functions:
                calls.add(called_proc)
        
        # Find function calls in assignments
        for match in self.function_call_pattern.finditer(proc_body):
            called_proc = self.clean_identifier(match.group(1))
            if called_proc.upper() not in self.builtin_functions:
                calls.add(called_proc)
        
        # Find direct procedure calls in expressions
        for match in self.expression_call_pattern.finditer(proc_body):
            called_proc = self.clean_identifier(match.group(1))
            if called_proc.upper() not in self.builtin_functions:
                calls.add(called_proc)
        
        # Find procedure calls in control structures
        for match in self.procedure_reference_pattern.finditer(proc_body):
            called_proc = self.clean_identifier(match.group(2))
            if called_proc.upper() not in self.builtin_functions:
                calls.add(called_proc)
        
        return list(calls)
    
    def find_external_calls(self, proc_body: str) -> Set[str]:
        """Find external system calls in procedure body"""
        external_calls = set()
        
        for system, pattern in self.external_patterns.items():
            matches = pattern.findall(proc_body)
            if matches:
                external_calls.update([f"{system}:{match}" for match in matches])
        
        return external_calls
    
    def analyze_directory(self, directory: str) -> None:
        """Analyze all TAL files in directory"""
        print(f"Analyzing TAL files in: {directory}")
        
        tal_files = self.find_tal_files(directory)
        print(f"Found {len(tal_files)} .TXT files")
        
        if not tal_files:
            print("No .TXT files found!")
            return
        
        for file_path in tal_files:
            print(f"Analyzing: {Path(file_path).name}")
            file_procedures = self.analyze_file(file_path)
            self.procedures.update(file_procedures)
        
        self.build_call_graph()
        
        print(f"\nAnalysis complete:")
        print(f"  Total procedures found: {len(self.procedures)}")
        print(f"  Total call relationships: {sum(len(calls) for calls in self.call_graph.values())}")
        print(f"  Procedures with external calls: {len(self.external_calls)}")
    
    def build_call_graph(self) -> None:
        """Build the call graph from procedure information"""
        for proc_name, proc_info in self.procedures.items():
            for called_proc in proc_info.calls:
                self.call_graph[proc_name].add(called_proc)
                self.reverse_graph[called_proc].add(proc_name)
                if called_proc in self.procedures:
                    self.procedures[called_proc].called_by.append(proc_name)
            
            # Mark leaf nodes (procedures with no outgoing calls)
            if not proc_info.calls:
                proc_info.is_leaf = True
    
    def generate_dot_graph(self, output_file: str = "call_graph.dot") -> None:
        """Generate DOT format graph (for Graphviz)"""
        with open(output_file, 'w') as f:
            f.write("digraph TAL_Call_Graph {\n")
            f.write("  rankdir=TB;\n")
            f.write("  node [shape=box, style=filled];\n")
            f.write("  edge [color=gray];\n\n")
            
            for proc_name, proc_info in self.procedures.items():
                if proc_info.is_leaf:
                    color = "green"
                    label = f"{proc_name}\\n[LEAF]"
                elif proc_info.is_external:
                    color = "red"
                    label = f"{proc_name}\\n[EXT]"
                elif len(proc_info.called_by) > 5:
                    color = "orange"
                    label = f"{proc_name}\\n[HUB:{len(proc_info.called_by)}]"
                elif len(proc_info.calls) > 5:
                    color = "yellow"
                    label = f"{proc_name}\\n[OUT:{len(proc_info.calls)}]"
                else:
                    color = "lightblue"
                    label = proc_name
                
                f.write(f'  "{proc_name}" [fillcolor={color}, label="{label}"];\n')
            
            f.write("\n")
            for caller, callees in self.call_graph.items():
                for callee in callees:
                    if callee in self.procedures:
                        f.write(f'  "{caller}" -> "{callee}";\n')
            
            f.write("}\n")
        
        print(f"DOT graph saved to: {output_file}")
        print("To generate PNG: dot -Tpng call_graph.dot -o call_graph.png")
        print("To generate SVG: dot -Tsvg call_graph.dot -o call_graph.svg")
    
    def generate_mermaid_graph(self, output_file: str = "call_graph.mmd") -> None:
        """Generate Mermaid format graph (for web rendering)"""
        with open(output_file, 'w') as f:
            f.write("graph TD\n")
            f.write("  classDef leaf fill:#4caf50,stroke:#333,stroke-width:2px\n")
            f.write("  classDef external fill:#ff6b6b,stroke:#333,stroke-width:2px\n")
            f.write("  classDef hub fill:#ffa726,stroke:#333,stroke-width:2px\n")
            f.write("  classDef caller fill:#ffeb3b,stroke:#333,stroke-width:2px\n")
            f.write("  classDef normal fill:#81c784,stroke:#333,stroke-width:2px\n\n")
            
            leaf_nodes = []
            external_nodes = []
            hub_nodes = []
            caller_nodes = []
            normal_nodes = []
            
            for proc_name, proc_info in self.procedures.items():
                safe_name = proc_name.replace("^", "_").replace("-", "_")
                if proc_info.is_leaf:
                    leaf_nodes.append(safe_name)
                    f.write(f"  {safe_name}[\"{proc_name}[LEAF]\"]\n")
                elif proc_info.is_external:
                    external_nodes.append(safe_name)
                    f.write(f"  {safe_name}[\"{proc_name}[EXT]\"]\n")
                elif len(proc_info.called_by) > 5:
                    hub_nodes.append(safe_name)
                    f.write(f"  {safe_name}[\"{proc_name}[HUB:{len(proc_info.called_by)}]\"]\n")
                elif len(proc_info.calls) > 5:
                    caller_nodes.append(safe_name)
                    f.write(f"  {safe_name}[\"{proc_name}[OUT:{len(proc_info.calls)}]\"]\n")
                else:
                    normal_nodes.append(safe_name)
                    f.write(f"  {safe_name}[\"{proc_name}\"]\n")
            
            f.write("\n")
            for caller, callees in self.call_graph.items():
                safe_caller = caller.replace("^", "_").replace("-", "_")
                for callee in callees:
                    if callee in self.procedures:
                        safe_callee = callee.replace("^", "_").replace("-", "_")
                        f.write(f"  {safe_caller} --> {safe_callee}\n")
            
            f.write("\n")
            if leaf_nodes:
                f.write(f"  class {','.join(leaf_nodes)} leaf\n")
            if external_nodes:
                f.write(f"  class {','.join(external_nodes)} external\n")
            if hub_nodes:
                f.write(f"  class {','.join(hub_nodes)} hub\n")
            if caller_nodes:
                f.write(f"  class {','.join(caller_nodes)} caller\n")
            if normal_nodes:
                f.write(f"  class {','.join(normal_nodes)} normal\n")
        
        print(f"Mermaid graph saved to: {output_file}")
        print("View online at: https://mermaid.live/")
    
    def generate_html_interactive(self, output_file: str = "call_graph.html") -> None:
        """Generate interactive HTML graph using vis.js"""
        nodes = []
        edges = []
        
        for proc_name, proc_info in self.procedures.items():
            if proc_info.is_leaf:
                color = "#4caf50"
                group = "leaf"
                label = f"{proc_name}\n[LEAF]"
            elif proc_info.is_external:
                color = "#ff6b6b"
                group = "external"
                label = f"{proc_name}\n[EXT]"
            elif len(proc_info.called_by) > 5:
                color = "#ffa726"
                group = "hub"
                label = f"{proc_name}\n[HUB:{len(proc_info.called_by)}]"
            elif len(proc_info.calls) > 5:
                color = "#ffeb3b"
                group = "caller"
                label = f"{proc_name}\n[OUT:{len(proc_info.calls)}]"
            else:
                color = "#81c784"
                group = "normal"
                label = proc_name
            
            nodes.append({
                "id": proc_name,
                "label": label,
                "color": color,
                "group": group,
                "title": f"File: {Path(proc_info.file_path).name}<br>Line: {proc_info.line_number}<br>Calls: {len(proc_info.calls)}<br>Called by: {len(proc_info.called_by)}"
            })
        
        for caller, callees in self.call_graph.items():
            for callee in callees:
                if callee in self.procedures:
                    edges.append({
                        "from": caller,
                        "to": callee,
                        "arrows": "to"
                    })
        
        html_content = f"""
<!DOCTYPE html>
<html>
<head>
    <title>TAL Procedure Call Graph</title>
    <script type="text/javascript" src="https://unpkg.com/vis-network/standalone/umd/vis-network.min.js"></script>
    <style>
        body {{ font-family: Arial, sans-serif; margin: 20px; }}
        #mynetwork {{ width: 100%; height: 800px; border: 1px solid lightgray; }}
        .legend {{ margin-top: 20px; }}
        .legend-item {{ display: inline-block; margin-right: 20px; }}
        .legend-color {{ width: 20px; height: 20px; display: inline-block; margin-right: 5px; }}
    </style>
</head>
<body>
    <h1>TAL Procedure Call Graph</h1>
    <p>Total procedures: {len(self.procedures)} | Total calls: {sum(len(calls) for calls in self.call_graph.values())}</p>
    
    <div id="mynetwork"></div>
    
    <div class="legend">
        <div class="legend-item">
            <span class="legend-color" style="background-color: #4caf50;"></span>
            Leaf Procedure (No Calls)
        </div>
        <div class="legend-item">
            <span class="legend-color" style="background-color: #81c784;"></span>
            Normal Procedure
        </div>
        <div class="legend-item">
            <span class="legend-color" style="background-color: #ffeb3b;"></span>
            High Fan-out (>5 calls)
        </div>
        <div class="legend-item">
            <span class="legend-color" style="background-color: #ffa726;"></span>
            High Fan-in (>5 callers)
        </div>
        <div class="legend-item">
            <span class="legend-color" style="background-color: #ff6b6b;"></span>
            External System Calls
        </div>
    </div>

    <script type="text/javascript">
        var nodes = new vis.DataSet({json.dumps(nodes, indent=2)});
        var edges = new vis.DataSet({json.dumps(edges, indent=2)});
        var data = {{ nodes: nodes, edges: edges }};
        var options = {{
            nodes: {{
                shape: 'box',
                font: {{ size: 12 }},
                borderWidth: 2,
                shadow: true
            }},
            edges: {{
                color: {{ color: '#848484' }},
                arrows: {{ to: {{ enabled: true, scaleFactor: 1 }} }},
                smooth: {{ type: 'continuous' }}
            }},
            physics: {{
                enabled: true,
                stabilization: {{ iterations: 100 }}
            }},
            interaction: {{
                hover: true,
                tooltipDelay: 200
            }}
        }};
        var network = new vis.Network(document.getElementById('mynetwork'), data, options);
    </script>
</body>
</html>
"""
        with open(output_file, 'w') as f:
            f.write(html_content)
        
        print(f"Interactive HTML graph saved to: {output_file}")
        print("Open in your web browser to view")
    
    def generate_text_report(self, output_file: str = "call_graph_report.txt") -> None:
        """Generate detailed text report with leaf nodes"""
        stats = self.generate_stats_report()
        
        with open(output_file, 'w') as f:
            f.write("TAL PROCEDURE CALL GRAPH ANALYSIS REPORT\n")
            f.write("=" * 50 + "\n\n")
            
            f.write("SUMMARY:\n")
            f.write(f"  Total procedures: {stats['summary']['total_procedures']}\n")
            f.write(f"  Total call relationships: {stats['summary']['total_calls']}\n")
            f.write(f"  Average calls per procedure: {stats['summary']['average_calls_per_procedure']:.1f}\n")
            f.write(f"  Procedures with external calls: {stats['summary']['procedures_with_external_calls']}\n")
            f.write(f"  Leaf procedures (no outgoing calls): {len(stats['leaf_procedures'])}\n\n")
            
            f.write("LEAF PROCEDURES (No Outgoing Calls):\n")
            f.write("-" * 50 + "\n")
            for proc in stats['leaf_procedures'][:20]:
                proc_info = self.procedures.get(proc, None)
                if proc_info:
                    f.write(f"  {proc:<30} | File: {Path(proc_info.file_path).name}\n")
            f.write("\n")
            
            f.write("CRITICAL PROCEDURES (High Fan-in):\n")
            f.write("-" * 50 + "\n")
            for proc, count in stats['top_called'][:15]:
                proc_info = self.procedures.get(proc, None)
                if proc_info:
                    f.write(f"  {proc:<30} | Called by {count:2d} | File: {Path(proc_info.file_path).name}\n")
            f.write("\n")
            
            f.write("COMPLEX PROCEDURES (High Fan-out):\n")
            f.write("-" * 50 + "\n")
            for proc, count in stats['top_callers'][:15]:
                proc_info = self.procedures.get(proc, None)
                if proc_info:
                    f.write(f"  {proc:<30} | Makes {count:2d} calls | File: {Path(proc_info.file_path).name}\n")
            f.write("\n")
            
            if stats['external_dependencies']:
                f.write("EXTERNAL SYSTEM INTEGRATIONS:\n")
                f.write("-" * 50 + "\n")
                for proc, deps in stats['external_dependencies'].items():
                    proc_info = self.procedures.get(proc, None)
                    if proc_info:
                        f.write(f"  {proc:<30} | File: {Path(proc_info.file_path).name}\n")
                        for dep in deps:
                            f.write(f"    -> {dep}\n")
                f.write("\n")
            
            if stats['isolated_procedures']:
                f.write("ISOLATED PROCEDURES (No calls in or out):\nThe following procedures are not called by any other procedures and do not make any calls themselves.\n")
                f.write("-" * 50 + "\n")
                for proc in stats['isolated_procedures'][:20]:
                    proc_info = self.procedures.get(proc, None)
                    if proc_info:
                        f.write(f"  {proc:<30} | File: {Path(proc_info.file_path).name}\n")
        
        print(f"Text report saved to: {output_file}")
    
    def generate_stats_report(self) -> Dict:
        """Generate statistics about the call graph"""
        stats = {
            "summary": {
                "total_procedures": len(self.procedures),
                "total_calls": sum(len(calls) for calls in self.call_graph.values()),
                "procedures_with_external_calls": len(self.external_calls),
                "average_calls_per_procedure": sum(len(calls) for calls in self.call_graph.values()) / len(self.procedures) if self.procedures else 0
            },
            "top_callers": [],
            "top_called": [],
            "leaf_procedures": [],
            "external_dependencies": dict(self.external_calls),
            "isolated_procedures": []
        }
        
        stats["top_callers"] = sorted(
            [(proc, len(calls)) for proc, calls in self.call_graph.items()],
            key=lambda x: x[1], reverse=True
        )[:10]
        
        stats["top_called"] = sorted(
            [(proc, len(callers)) for proc, callers in self.reverse_graph.items()],
            key=lambda x: x[1], reverse=True
        )[:10]
        
        for proc_name, proc_info in self.procedures.items():
            if proc_info.is_leaf:
                stats["leaf_procedures"].append(proc_name)
            if (proc_name not in self.call_graph or len(self.call_graph[proc_name]) == 0) and \
               (proc_name not in self.reverse_graph or len(self.reverse_graph[proc_name]) == 0):
                stats["isolated_procedures"].append(proc_name)
        
        return stats
    
    def save_results(self, output_dir: str) -> None:
        """Save analysis results to files"""
        output_path = Path(output_dir)
        output_path.mkdir(exist_ok=True)
        
        proc_data = {}
        for name, info in self.procedures.items():
            proc_data[name] = {
                "file_path": info.file_path,
                "line_number": info.line_number,
                "calls": info.calls,
                "called_by": info.called_by,
                "is_external": info.is_external,
                "is_leaf": info.is_leaf
            }
        
        with open(output_path / "procedures.json", 'w') as f:
            json.dump(proc_data, f, indent=2)
        
        call_graph_data = {caller: list(callees) for caller, callees in self.call_graph.items()}
        with open(output_path / "call_graph.json", 'w') as f:
            json.dump(call_graph_data, f, indent=2)
        
        stats = self.generate_stats_report()
        with open(output_path / "statistics.json", 'w') as f:
            json.dump(stats, f, indent=2)
        
        print(f"Results saved to: {output_path}")

def main():
    parser = argparse.ArgumentParser(description="Generate TAL procedure call graph")
    parser.add_argument("directory", help="Directory containing .TXT TAL source files")
    parser.add_argument("--output", "-o", default="tal_analysis", 
                       help="Output directory for results")
    parser.add_argument("--format", choices=["html", "dot", "mermaid", "text", "all"],
                       default="all", help="Output format")
    parser.add_argument("--stats-only", action="store_true",
                       help="Generate statistics only, no visualization")
    
    args = parser.parse_args()
    
    if not os.path.exists(args.directory):
        print(f"Error: Directory {args.directory} does not exist")
        return 1
    
    analyzer = TALCallGraphAnalyzer()
    analyzer.analyze_directory(args.directory)
    
    if len(analyzer.procedures) == 0:
        print("No procedures found in the source files!")
        return 1
    
    analyzer.save_results(args.output)
    
    if not args.stats_only:
        output_path = Path(args.output)
        if args.format == "all" or args.format == "html":
            analyzer.generate_html_interactive(str(output_path / "call_graph.html"))
        if args.format == "all" or args.format == "dot":
            analyzer.generate_dot_graph(str(output_path / "call_graph.dot"))
        if args.format == "all" or args.format == "mermaid":
            analyzer.generate_mermaid_graph(str(output_path / "call_graph.mmd"))
        if args.format == "all" or args.format == "text":
            analyzer.generate_text_report(str(output_path / "call_graph_report.txt"))
    
    stats = analyzer.generate_stats_report()
    print(f"\n=== Call Graph Analysis Summary ===")
    print(f"Total procedures: {stats['summary']['total_procedures']}")
    print(f"Total call relationships: {stats['summary']['total_calls']}")
    print(f"Average calls per procedure: {stats['summary']['average_calls_per_procedure']:.1f}")
    print(f"Procedures with external calls: {stats['summary']['procedures_with_external_calls']}")
    print(f"Leaf procedures: {len(stats['leaf_procedures'])}")
    
    if stats['leaf_procedures']:
        print(f"\nLeaf Procedures (No Outgoing Calls):")
        for proc in stats['leaf_procedures'][:5]:
            proc_info = analyzer.procedures.get(proc)
            print(f"  {proc}: File {Path(proc_info.file_path).name}")
    
    if stats['top_callers']:
        print(f"\nTop Callers (High Fan-out):")
        for proc, count in stats['top_callers'][:5]:
            print(f"  {proc}: {count} calls")
    
    if stats['top_called']:
        print(f"\nMost Called Procedures (High Fan-in):")
        for proc, count in stats['top_called'][:5]:
            print(f"  {proc}: called by {count} procedures")
    
    if stats['external_dependencies']:
        print(f"\nProcedures with External Dependencies:")
        for proc, deps in list(stats['external_dependencies'].items())[:5]:
            print(f"  {proc}: {', '.join(list(deps)[:3])}")
    
    return 0

if __name__ == "__main__":
    exit(main())