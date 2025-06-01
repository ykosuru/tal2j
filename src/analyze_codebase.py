#!/usr/bin/env python3
"""
Simple TAL Complexity Analyzer
Scans folders, analyzes procedures one by one, rates complexity based on external dependencies
"""


########### Complexity Scoring System ###############################
# This is a simplified complexity scoring system for TAL procedures.
#base_complexity = 1  # Every procedure starts with 1 point

# Control Flow Patterns (+1 point each):
#- IF statements
#- CASE statements  
#- WHILE loops
#- FOR loops
#- DO loops

# Function Calls (+1 point each):
#- CALL statements (any procedure calls)

# Nested Blocks:
#- BEGIN blocks (minus 1 for the main procedure BEGIN)

# External Dependencies (+5 points each):
#- TANDEM system calls
#- IFT3TAL system calls
#- IFT3DDLS system calls
#- NSCRIBE system calls
#- Guardian system calls
#- Pathway system calls
#- SQL queries (if applicable)

# High Complexity External Systems (+10 points each):
#- TANDEM system calls
#- IFT3DDLS system calls
#- Guardian system calls
#- SQL queries
#####################################################################

import os
import re
import json
import argparse
from pathlib import Path
from typing import Dict, List, Optional
from dataclasses import dataclass

@dataclass
class ProcedureInfo:
    """Information about a single TAL procedure"""
    name: str
    file_path: str
    start_line: int
    end_line: int
    line_count: int
    complexity_score: int
    external_dependencies: List[str]
    self_contained: bool
    ast_convertible: bool

@dataclass
class FileInfo:
    """Information about a single TAL file"""
    path: str
    procedures: List[ProcedureInfo]
    external_includes: List[str]
    total_complexity: int

class SimpleTALAnalyzer:
    """Simple TAL analyzer focusing on external dependencies"""
    
    def __init__(self):
        # External system patterns
        self.external_patterns = {
            'TANDEM': re.compile(r'\bTANDEM_[A-Z0-9_^]+\b', re.IGNORECASE),
            'IFT3TAL': re.compile(r'\bIFT3TAL_[A-Z0-9_^]+\b', re.IGNORECASE),
            'IFT3DDLS': re.compile(r'\bIFT3DDLS_[A-Z0-9_^]+\b', re.IGNORECASE),
            'NSCRIBE': re.compile(r'\b(?:N|EN)SCRIBE[A-Z0-9_^]*\b', re.IGNORECASE),
            'GUARDIAN': re.compile(r'\b(?:GUARDIAN|PROCESS_|SYSTEM_)[A-Z0-9_^]+\b', re.IGNORECASE),
            'PATHWAY': re.compile(r'\b(?:PATHWAY|SERVERCLASS|REQUESTOR)[A-Z0-9_^]*\b', re.IGNORECASE),
            'SQL': re.compile(r'\b(?:SELECT|INSERT|UPDATE|DELETE|CREATE|DROP|ALTER|EXEC|SQL)\b', re.IGNORECASE)
        }
        
        # Source directive patterns for external includes
        self.include_patterns = {
            'TANDEM_SOURCE': re.compile(r'^\s*\?\s*source\s*=?\s*(TANDEM_[A-Z0-9_^]+)', re.IGNORECASE | re.MULTILINE),
            'IFT3TAL_SOURCE': re.compile(r'^\s*\?\s*source\s*=?\s*(IFT3TAL_[A-Z0-9_^]+)', re.IGNORECASE | re.MULTILINE),
            'IFT3DDLS_SOURCE': re.compile(r'^\s*\?\s*source\s*=?\s*(IFT3DDLS_[A-Z0-9_^]+)', re.IGNORECASE | re.MULTILINE),
            'GENERAL_SOURCE': re.compile(r'^\s*\?\s*(?:source|decs|library)\s*=?\s*([A-Z0-9_^]+)', re.IGNORECASE | re.MULTILINE)
        }
        
        # Procedure detection
        self.proc_start_pattern = re.compile(
            r'^\s*(?:(int|string|real|fixed)\s+)?(proc|subproc)\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*(?:\([^)]*\))?\s*;?\s*$',
            re.IGNORECASE | re.MULTILINE
        )
        
        # Complexity indicators
        self.complexity_patterns = {
            'control_flow': re.compile(r'\b(?:if|case|while|for|do)\b', re.IGNORECASE),
            'calls': re.compile(r'\bcall\s+[a-zA-Z^][a-zA-Z0-9^_]*', re.IGNORECASE),
            'nested_blocks': re.compile(r'\bbegin\b', re.IGNORECASE)
        }
    
    def find_tal_files(self, folder_path: str) -> List[str]:
        """Find all TAL files in folder"""
        tal_files = []
        folder = Path(folder_path)
        
        if not folder.exists():
            print(f"Error: Folder {folder_path} does not exist")
            return []
        
        # Look for common TAL file extensions
        extensions = ['.tal', '.t', '.proc', '.h', '.def']
        
        for file_path in folder.rglob('*'):
            if file_path.is_file():
                if (file_path.suffix.lower() in extensions or
                    any(keyword in file_path.name.lower() for keyword in ['tal', 'proc'])):
                    tal_files.append(str(file_path))
        
        return sorted(tal_files)
    
    def analyze_file(self, file_path: str) -> Optional[FileInfo]:
        """Analyze a single TAL file"""
        try:
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
            
            # Find external includes
            external_includes = self._find_external_includes(content)
            
            # Find and analyze procedures
            procedures = self._analyze_procedures(content, file_path)
            
            # Calculate total file complexity
            total_complexity = sum(proc.complexity_score for proc in procedures)
            
            return FileInfo(
                path=file_path,
                procedures=procedures,
                external_includes=external_includes,
                total_complexity=total_complexity
            )
            
        except Exception as e:
            print(f"Error analyzing {file_path}: {e}")
            return None
    
    def _find_external_includes(self, content: str) -> List[str]:
        """Find external system includes in source directives"""
        external_includes = []
        
        for pattern_name, pattern in self.include_patterns.items():
            matches = pattern.findall(content)
            for match in matches:
                if pattern_name != 'GENERAL_SOURCE':
                    external_includes.append(f"{pattern_name}: {match}")
                else:
                    # Check if general source is external
                    if any(keyword in match.upper() for keyword in ['TANDEM', 'IFT3', 'NSCRIBE', 'GUARDIAN']):
                        external_includes.append(f"EXTERNAL: {match}")
        
        return list(set(external_includes))  # Remove duplicates
    
    def _analyze_procedures(self, content: str, file_path: str) -> List[ProcedureInfo]:
        """Find and analyze all procedures in the file"""
        procedures = []
        lines = content.split('\n')
        
        # Find procedure starts
        proc_matches = list(self.proc_start_pattern.finditer(content))
        
        for i, match in enumerate(proc_matches):
            proc_name = match.group(3).replace('^', '_')
            start_line = content[:match.start()].count('\n') + 1
            
            # Find procedure end
            if i + 1 < len(proc_matches):
                # Next procedure starts
                next_start = content[:proc_matches[i + 1].start()].count('\n') + 1
                end_line = next_start - 1
            else:
                # Last procedure - goes to end of file
                end_line = len(lines)
            
            # Extract procedure content
            proc_content = '\n'.join(lines[start_line-1:end_line])
            
            # Analyze this procedure
            proc_info = self._analyze_single_procedure(
                proc_name, proc_content, file_path, start_line, end_line
            )
            
            procedures.append(proc_info)
        
        return procedures
    
    def _analyze_single_procedure(self, name: str, content: str, file_path: str, 
                                start_line: int, end_line: int) -> ProcedureInfo:
        """Analyze a single procedure for complexity and dependencies"""
        
        line_count = end_line - start_line + 1
        
        # Find external dependencies
        external_deps = []
        for dep_type, pattern in self.external_patterns.items():
            matches = pattern.findall(content)
            if matches:
                external_deps.extend([f"{dep_type}: {match}" for match in matches])
        
        # Calculate base complexity from control flow
        base_complexity = 1  # Start with 1
        for complexity_type, pattern in self.complexity_patterns.items():
            matches = len(pattern.findall(content))
            if complexity_type == 'nested_blocks':
                base_complexity += max(0, matches - 1)  # Subtract main begin
            else:
                base_complexity += matches
        
        # Calculate external dependency penalty
        external_penalty = len(external_deps) * 5  # 5 points per external dependency
        
        # Special penalties for high-complexity external systems
        high_complexity_systems = ['TANDEM', 'IFT3DDLS', 'GUARDIAN', 'SQL']
        for dep in external_deps:
            for system in high_complexity_systems:
                if system in dep:
                    external_penalty += 3  # Additional 3 points for complex systems
        
        # Total complexity score
        complexity_score = base_complexity + external_penalty
        
        # Determine if self-contained (no external dependencies)
        self_contained = len(external_deps) == 0
        
        # Determine if AST convertible (low complexity, self-contained)
        ast_convertible = self_contained and base_complexity <= 10 and line_count <= 50
        
        return ProcedureInfo(
            name=name,
            file_path=file_path,
            start_line=start_line,
            end_line=end_line,
            line_count=line_count,
            complexity_score=complexity_score,
            external_dependencies=external_deps,
            self_contained=self_contained,
            ast_convertible=ast_convertible
        )
    
    def analyze_folder(self, folder_path: str) -> Dict:
        """Analyze entire folder and return results"""
        print(f"Analyzing TAL files in: {folder_path}")
        
        # Find all TAL files
        tal_files = self.find_tal_files(folder_path)
        print(f"Found {len(tal_files)} TAL files")
        
        if not tal_files:
            return {"error": "No TAL files found"}
        
        # Analyze each file
        all_files = []
        all_procedures = []
        
        for file_path in tal_files:
            print(f"Analyzing: {file_path}")
            file_info = self.analyze_file(file_path)
            
            if file_info:
                all_files.append(file_info)
                all_procedures.extend(file_info.procedures)
        
        # Generate summary statistics
        total_procedures = len(all_procedures)
        self_contained_procs = [p for p in all_procedures if p.self_contained]
        ast_convertible_procs = [p for p in all_procedures if p.ast_convertible]
        high_complexity_procs = [p for p in all_procedures if p.complexity_score > 20]
        
        # Categorize procedures by complexity
        complexity_categories = {
            'simple': [p for p in all_procedures if p.complexity_score <= 10],
            'moderate': [p for p in all_procedures if 10 < p.complexity_score <= 20],
            'complex': [p for p in all_procedures if 20 < p.complexity_score <= 40],
            'very_complex': [p for p in all_procedures if p.complexity_score > 40]
        }
        
        # Find most common external dependencies
        all_external_deps = []
        for proc in all_procedures:
            all_external_deps.extend(proc.external_dependencies)
        
        from collections import Counter
        common_external_deps = Counter(all_external_deps).most_common(10)
        
        # Create detailed procedure list for easy reference
        procedure_details = []
        for proc in all_procedures:
            file_name = Path(proc.file_path).name
            procedure_details.append({
                "filename": file_name,
                "full_path": proc.file_path,
                "procedure_name": proc.name,
                "self_contained": proc.self_contained,
                "has_external_calls": len(proc.external_dependencies) > 0,
                "external_dependencies": proc.external_dependencies,
                "complexity_score": proc.complexity_score,
                "ast_convertible": proc.ast_convertible,
                "line_count": proc.line_count,
                "lines": f"{proc.start_line}-{proc.end_line}",
                "category": self._get_complexity_category(proc.complexity_score)
            })
        
        # Sort procedures by complexity (self-contained first, then by score)
        procedure_details.sort(key=lambda x: (not x["self_contained"], x["complexity_score"]))
        
        return {
            "summary": {
                "total_files": len(all_files),
                "total_procedures": total_procedures,
                "self_contained_procedures": len(self_contained_procs),
                "procedures_with_external_calls": total_procedures - len(self_contained_procs),
                "ast_convertible_procedures": len(ast_convertible_procs),
                "high_complexity_procedures": len(high_complexity_procs),
                "self_contained_percentage": round(len(self_contained_procs) / total_procedures * 100, 1) if total_procedures > 0 else 0,
                "ast_convertible_percentage": round(len(ast_convertible_procs) / total_procedures * 100, 1) if total_procedures > 0 else 0
            },
            "complexity_distribution": {
                category: len(procs) for category, procs in complexity_categories.items()
            },
            "common_external_dependencies": dict(common_external_deps),
            "processing_recommendations": self._generate_processing_recommendations(complexity_categories, self_contained_procs, ast_convertible_procs),
            "procedure_details": procedure_details,
            "files": [
                {
                    "filename": Path(f.path).name,
                    "full_path": f.path,
                    "total_complexity": f.total_complexity,
                    "procedure_count": len(f.procedures),
                    "external_includes": f.external_includes,
                    "procedures": [
                        {
                            "name": p.name,
                            "lines": f"{p.start_line}-{p.end_line}",
                            "line_count": p.line_count,
                            "complexity_score": p.complexity_score,
                            "external_dependencies": p.external_dependencies,
                            "self_contained": p.self_contained,
                            "has_external_calls": len(p.external_dependencies) > 0,
                            "ast_convertible": p.ast_convertible,
                            "category": self._get_complexity_category(p.complexity_score)
                        }
                        for p in f.procedures
                    ]
                }
                for f in all_files
            ]
        }
    
    def _get_complexity_category(self, score: int) -> str:
        """Get complexity category name for a score"""
        if score <= 10:
            return "simple"
        elif score <= 20:
            return "moderate"
        elif score <= 40:
            return "complex"
        else:
            return "very_complex"
    
    def _generate_processing_recommendations(self, complexity_categories: Dict, 
                                           self_contained_procs: List, 
                                           ast_convertible_procs: List) -> Dict:
        """Generate processing recommendations based on analysis"""
        
        total_procs = sum(len(procs) for procs in complexity_categories.values())
        
        recommendations = {
            "ast_transpiler_candidates": len(ast_convertible_procs),
            "llm_processing_candidates": len(complexity_categories['complex']) + len(complexity_categories['very_complex']),
            "batch_sizes": {
                "ast_convertible": 50,  # Can process many at once
                "simple_self_contained": 20,  # Medium batch size
                "moderate_complexity": 10,  # Smaller batches
                "high_complexity": 3,  # Individual attention
                "external_heavy": 1  # One at a time
            },
            "processing_strategy": "tiered" if total_procs > 100 else "sequential",
            "priority_order": [
                "ast_convertible (fastest, cheapest)",
                "simple_self_contained (AST + light LLM)",
                "moderate_complexity (LLM with context)",
                "external_heavy (LLM with external system knowledge)",
                "very_complex (manual review + LLM)"
            ]
        }
        
        if len(ast_convertible_procs) > total_procs * 0.5:
            recommendations["bulk_processing_suitable"] = True
        
        if len(complexity_categories['very_complex']) > total_procs * 0.2:
            recommendations["expert_review_needed"] = True
        
        return recommendations

def main():
    parser = argparse.ArgumentParser(description="Simple TAL Complexity Analyzer")
    parser.add_argument("folder", help="Folder containing TAL files")
    parser.add_argument("--output", "-o", default="tal_analysis.json", 
                       help="Output JSON file (default: tal_analysis.json)")
    parser.add_argument("--verbose", "-v", action="store_true", 
                       help="Verbose output")
    
    args = parser.parse_args()
    
    # Run analysis
    analyzer = SimpleTALAnalyzer()
    results = analyzer.analyze_folder(args.folder)
    
    # Save results
    with open(args.output, 'w') as f:
        json.dump(results, f, indent=2)
    
    # Print summary
    if "error" not in results:
        summary = results["summary"]
        print(f"\nAnalysis Complete!")
        print(f"=" * 50)
        print(f"Files analyzed: {summary['total_files']}")
        print(f"Procedures found: {summary['total_procedures']}")
        print(f"Self-contained procedures: {summary['self_contained_procedures']} ({summary['self_contained_percentage']}%)")
        print(f"AST convertible procedures: {summary['ast_convertible_procedures']} ({summary['ast_convertible_percentage']}%)")
        print(f"High complexity procedures: {summary['high_complexity_procedures']}")
        
        print(f"\nComplexity Distribution:")
        for category, count in results["complexity_distribution"].items():
            print(f"  {category.title()}: {count}")
        
        print(f"\nTop External Dependencies:")
        for dep, count in list(results["common_external_dependencies"].items())[:5]:
            print(f"  {dep}: {count}")
        
        print(f"\nProcessing Recommendations:")
        recs = results["processing_recommendations"]
        print(f"  AST transpiler candidates: {recs['ast_transpiler_candidates']}")
        print(f"  LLM processing candidates: {recs['llm_processing_candidates']}")
        print(f"  Processing strategy: {recs['processing_strategy']}")
        
        if recs.get('bulk_processing_suitable'):
            print(f"  ✓ Bulk processing suitable (many self-contained procedures)")
        
        if recs.get('expert_review_needed'):
            print(f"  ⚠ Expert review needed (many very complex procedures)")
        
        print(f"\nResults saved to: {args.output}")
    else:
        print(f"Error: {results['error']}")

if __name__ == "__main__":
    main()