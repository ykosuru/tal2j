# talTranspiler.py

import uuid
from antlr4 import *
from antlr4.error.ErrorListener import ErrorListener
from antlr4.tree.Tree import TerminalNode
import re
import sys
import os
import json

# Import the generated Visitor and Parser
try:
    from TALVisitor import TALVisitor
    from TALParser import TALParser
    from TALLexer import TALLexer
except ImportError as e:
    print(f"Error: ANTLR generated files not found: {e}")
    print("Please generate them using: antlr4 -Dlanguage=Python3 TAL.g4 -visitor -no-listener")
    sys.exit(1)

class ASTNode:
    def __init__(self, node_type, text="", children=None, line_number=0):
        self.node_type = node_type
        self.text = text
        self.children = children or []
        self.line_number = line_number
        self.attributes = {}
        self.attributes["_preprocessed"] = False
        self.attributes["_preprocessor_pattern_id"] = None

    def add_child(self, child):
        if child:
            self.children.append(child)

    def to_dict(self):
        return {
            "type": self.node_type,
            "text": self.text,
            "line": self.line_number,
            "attributes": self.attributes,
            "children": [child.to_dict() for child in self.children if child]
        }

class TALPreprocessor:
    def __init__(self, patterns_file="tal_codepairs.json"):
        self.patterns = []
        if os.path.exists(patterns_file):
            try:
                with open(patterns_file, 'r', encoding='utf-8') as f:
                    self.patterns = json.load(f)
            except json.JSONDecodeError as e:
                print(f"Warning: Could not decode JSON from '{patterns_file}'. Error: {e}", file=sys.stderr)
            except Exception as e:
                print(f"Warning: Could not load patterns from '{patterns_file}'. Error: {e}", file=sys.stderr)
        else:
            print(f"Info: Preprocessor patterns file '{patterns_file}' not found. Using built-in patterns only.", file=sys.stderr)

    def clean_tal_identifier(self, identifier):
        if not identifier:
            return identifier
        return identifier.replace('^', '_')

    def preprocess_lines(self, lines):
        output_with_line_info = []
        current_original_line_idx = 0

        while current_original_line_idx < len(lines):
            original_line_text = lines[current_original_line_idx]
            current_tal_line_num = current_original_line_idx + 1

            matched_by_preprocessor = False

            # Check for multi-line constructs first
            for pattern_info in self.patterns:
                pattern_id = pattern_info.get("id", "unknown_pattern")
                pattern_type = pattern_info.get("type")

                if pattern_type == "multi_line_construct":
                    start_regex = pattern_info.get("match_pattern_start")
                    if not start_regex:
                        continue
                    
                    match_start = re.match(start_regex, original_line_text.strip(), re.IGNORECASE)
                    if match_start:
                        handler_name = pattern_info.get("handler")
                        handler_func = getattr(self, handler_name, None)
                        if handler_func:
                            node, lines_consumed = handler_func(lines, current_original_line_idx, pattern_info)
                            if node:
                                node.attributes["_preprocessed"] = True
                                node.attributes["_preprocessor_pattern_id"] = pattern_id
                                output_with_line_info.append((node, current_tal_line_num, lines_consumed))
                                current_original_line_idx += lines_consumed
                                matched_by_preprocessor = True
                                break
                        else:
                            print(f"Warning: Preprocessor handler '{handler_name}' not found for pattern '{pattern_id}'.", file=sys.stderr)

            if not matched_by_preprocessor:
                output_with_line_info.append((original_line_text, current_tal_line_num, 1))
                current_original_line_idx += 1

        return output_with_line_info

class CustomTALVisitor(TALVisitor):
    def __init__(self, line_number_offset=0):
        super().__init__()
        self.current_line_offset = line_number_offset 

    def _get_original_line(self, ctx_or_symbol):
        line_in_stream = 0
        if hasattr(ctx_or_symbol, 'start'):
            line_in_stream = ctx_or_symbol.start.line
        elif hasattr(ctx_or_symbol, 'line'):
            line_in_stream = ctx_or_symbol.line
        else:
            return self.current_line_offset + 1

        return line_in_stream + self.current_line_offset
    

    def clean_tal_identifier(self, identifier_text):
        if not identifier_text:
            return identifier_text
        return identifier_text.replace('^', '_')

    def _create_node(self, ctx_or_symbol, node_type_name=None, text_override=None):
        node_type = node_type_name
        original_text_for_node = text_override
        line_num_for_node = self._get_original_line(ctx_or_symbol)

        if not node_type_name and hasattr(ctx_or_symbol, '__class__'):
            node_type = ctx_or_symbol.__class__.__name__.replace('Context', '')
        
        if text_override is None and hasattr(ctx_or_symbol, 'getText'):
            original_text_for_node = ctx_or_symbol.getText()
        elif text_override is None and hasattr(ctx_or_symbol, 'text'):
             original_text_for_node = ctx_or_symbol.text

        display_text = original_text_for_node.split('\n')[0].strip() if original_text_for_node and '\n' in original_text_for_node else (original_text_for_node.strip() if original_text_for_node else "")
        
        node = ASTNode(node_type if node_type else "UnknownVisitorNode", display_text, line_number=line_num_for_node)
        node.attributes["full_text_from_parser"] = original_text_for_node
        return node
    
    def visit(self, tree):
        """Override visit to provide better error context"""
        try:
            return super().visit(tree)
        except Exception as e:
            # Create an error node with context
            if hasattr(tree, 'getText'):
                text = tree.getText()[:50]  # First 50 chars
            else:
                text = str(tree)[:50]
            
            error_node = ASTNode("VisitorError", text, line_number=self._get_original_line(tree))
            error_node.attributes["error_message"] = str(e)
            error_node.attributes["error_type"] = type(e).__name__
            return error_node
        
    def visitChildren(self, node):
        """Override to provide better error handling"""
        try:
            return super().visitChildren(node)
        except Exception as e:
            # If we can't visit children, create an error node
            error_node = ASTNode("VisitError", str(e), line_number=0)
            return error_node
    
    def visitProgramElement(self, ctx):
        if ctx.directiveLine():
            return self.visit(ctx.directiveLine())
        if ctx.topLevelDeclaration():
            return self.visit(ctx.topLevelDeclaration())
        if ctx.procedureDefinition():
            return self.visit(ctx.procedureDefinition())
        return None

    def visitDirectiveLine(self, ctx):
        node = self._create_node(ctx, text_override=ctx.getText().strip())
        directives_attr = [de_ctx.getText().strip() for de_ctx in ctx.directiveElement()]
        node.attributes["directives"] = directives_attr
        
        for de_ctx in ctx.directiveElement():
            child_node = self.visit(de_ctx)
            if child_node:
                node.add_child(child_node)
        return node

    def visitTopLevelDeclaration(self, ctx):
        if ctx.literalDeclaration():
            return self.visit(ctx.literalDeclaration())
        if ctx.variableDeclaration():
            return self.visit(ctx.variableDeclaration())
        if ctx.structDeclaration():
            return self.visit(ctx.structDeclaration())
        if ctx.nameDeclaration():
            node = self._create_node(ctx.nameDeclaration(), "nameDeclaration")
            node.attributes["name_declared"] = self.clean_tal_identifier(ctx.nameDeclaration().IDENTIFIER().getText())
            return node
        if ctx.blockDeclaration():
            return self.visit(ctx.blockDeclaration())
        return None

    def visitLiteralDeclaration(self, ctx):
        node = self._create_node(ctx, text_override=ctx.getText().strip())
        items = []
        for item_ctx in ctx.literalItem():
            name = self.clean_tal_identifier(item_ctx.IDENTIFIER().getText())
            value_text = item_ctx.expression().getText()
            items.append({"name": name, "value": value_text})
        node.attributes["items"] = items
        return node

    def visitVariableDeclaration(self, ctx):
        node = self._create_node(ctx, text_override=ctx.getText().strip())
        
        type_spec_text = ctx.typeSpecifier().getText()
        node.attributes["type"] = type_spec_text
        
        variables_data = []
        for var_decl_ctx in ctx.variableDeclarator():
            var_name = self.clean_tal_identifier(var_decl_ctx.IDENTIFIER().getText())
            indirection = bool(var_decl_ctx.indirectionSpecifier())
            array_spec = var_decl_ctx.arraySpecifier().getText() if var_decl_ctx.arraySpecifier() else None
            init_val = var_decl_ctx.expression().getText() if var_decl_ctx.ASSIGN() and var_decl_ctx.expression() else None
            
            var_info = {"name": var_name, "indirection": indirection}
            if array_spec:
                var_info["array_spec"] = array_spec
            if init_val:
                var_info["init_value"] = init_val
            variables_data.append(var_info)
        
        node.attributes["variables"] = variables_data
        return node

    def visitBlockDeclaration(self, ctx):
        node = self._create_node(ctx, text_override=f"BLOCK {ctx.IDENTIFIER().getText()};")
        node.attributes["name"] = self.clean_tal_identifier(ctx.IDENTIFIER().getText())
        
        for child_item_ctx in ctx.children:
            if hasattr(child_item_ctx, 'accept') and hasattr(child_item_ctx, '__class__'):
                class_name = child_item_ctx.__class__.__name__
                if 'Declaration' in class_name or 'Procedure' in class_name or 'Statement' in class_name:
                    child_node = self.visit(child_item_ctx)
                    if child_node:
                        node.add_child(child_node)
            elif isinstance(child_item_ctx, TerminalNode):
                symbol_type = getattr(child_item_ctx.symbol, 'type', None)
                if symbol_type in [TALParser.END_KW, TALParser.ENDBLOCK_KW]:
                    end_text = child_item_ctx.getText()
                    if ctx.COMMENT():
                        end_text += " " + ctx.COMMENT().getText()
                    node.add_child(ASTNode("EndBlockStatement", end_text.strip(), line_number=self._get_original_line(child_item_ctx.symbol)))
        
        return node

    def visitProcedureDefinition(self, ctx):
        proc_type_kw = ctx.PROC_KW() or ctx.SUBPROC_KW()
        proc_type = proc_type_kw.getText().upper()
        proc_name = self.clean_tal_identifier(ctx.IDENTIFIER().getText())
        params_str = ctx.formalParameterList().getText() if ctx.formalParameterList() else ""
        
        node = self._create_node(ctx, text_override=f"{proc_type} {ctx.IDENTIFIER().getText()}({params_str});")
        node.attributes["name"] = proc_name
        node.attributes["type"] = proc_type
        node.attributes["parameters_text"] = params_str

        for param_type_decl_ctx in ctx.formalParameterTypeDeclaration():
            param_node = self.visit(param_type_decl_ctx)
            if param_node:
                node.add_child(param_node)
        
        if ctx.procedureBody():
            body_node = self.visit(ctx.procedureBody())
            if body_node:
                node.add_child(body_node)
        
        return node

    def visitProcedureBody(self, ctx):
        node = self._create_node(ctx, "procedureBody", text_override="BEGIN...END_PROC_BODY")
        
        for decl_ctx in ctx.declaration():
            child_node = self.visit(decl_ctx)
            if child_node:
                node.add_child(child_node)
        
        for stmt_ctx in ctx.statement():
            child_node = self.visit(stmt_ctx)
            if child_node:
                node.add_child(child_node)
        
        return node

    def visitStatement(self, ctx):
        if ctx.assignmentStatement():
            return self.visit(ctx.assignmentStatement())
        if ctx.ifStatement():
            return self.visit(ctx.ifStatement())
        if ctx.whileStatement():
            return self.visit(ctx.whileStatement())
        if ctx.callStatement():
            return self.visit(ctx.callStatement())
        if ctx.returnStatement():
            return self.visit(ctx.returnStatement())
        if ctx.blockStatement():
            return self.visit(ctx.blockStatement())
        if ctx.directiveLine():
            return self.visit(ctx.directiveLine())
        if ctx.emptyStatement():
            return ASTNode("emptyStatement", ";", line_number=self._get_original_line(ctx))
        if ctx.expression():
            expr_node = self.visit(ctx.expression())
            stmt_node = ASTNode("expressionStatement", ctx.expression().getText().strip(), line_number=self._get_original_line(ctx.expression()))
            if expr_node:
                stmt_node.add_child(expr_node)
            return stmt_node
        # NEW: Handle new statements
        if ctx.assertStatement():
            return self.visit(ctx.assertStatement())
        if ctx.caseStatement():
            return self.visit(ctx.caseStatement())
        if ctx.dropStatement():
            return self.visit(ctx.dropStatement())
        if ctx.gotoStatement():
            return self.visit(ctx.gotoStatement())
        if ctx.rscanStatement():
            return self.visit(ctx.rscanStatement())
        if ctx.scanStatement():
            return self.visit(ctx.scanStatement())
        if ctx.storeStatement():
            return self.visit(ctx.storeStatement())
        if ctx.useStatement():
            return self.visit(ctx.useStatement())
        
        return ASTNode("Statement", ctx.getText().strip(), line_number=self._get_original_line(ctx))

    def visitCallStatement(self, ctx):
        node = self._create_node(ctx, text_override=ctx.getText().strip())
        func_name = self.clean_tal_identifier(ctx.IDENTIFIER().getText())
        node.attributes["function"] = func_name
        node.attributes["call_text"] = ctx.getText().strip()
        
        params_attr_list = []
        if ctx.actualParameterList():
            for expr_ctx in ctx.actualParameterList().expression():
                param_node = self.visit(expr_ctx)
                if param_node:
                    node.add_child(param_node)
                    params_attr_list.append({
                        "node_type": param_node.node_type, 
                        "text": param_node.text, 
                        "line": param_node.line_number
                    })
        
        node.attributes["parameters"] = params_attr_list
        return node

    def visitIfStatement(self, ctx):
        node = self._create_node(ctx, "ifStatement", text_override=f"IF {ctx.expression().getText()} THEN...")
        node.attributes["condition"] = ctx.expression().getText().strip()
        node.add_child(self.visit(ctx.expression()))
        
        then_body_ast_node = ASTNode("thenBody", "", line_number=self._get_original_line(ctx.THEN_STMT().symbol))
        if len(ctx.statement()) > 0:
            then_body_ast_node.add_child(self.visit(ctx.statement(0)))
        node.add_child(then_body_ast_node)

        if ctx.ELSE_STMT():
            else_body_ast_node = ASTNode("elseBody", "", line_number=self._get_original_line(ctx.ELSE_STMT().symbol))
            if len(ctx.statement()) > 1:
                else_body_ast_node.add_child(self.visit(ctx.statement(1)))
            node.add_child(else_body_ast_node)
        
        if ctx.ENDIF_STMT():
            node.add_child(self._create_node(ctx.ENDIF_STMT().getSymbol(), "EndIfMarker"))
        
        return node

    # NEW: Visitor methods for new statements
    def visitAssertStatement(self, ctx):
        node = self._create_node(ctx, "assertStatement", text_override=ctx.getText().strip())
        node.attributes["condition"] = ctx.expression().getText().strip()
        expr_node = self.visit(ctx.expression())
        if expr_node:
            node.add_child(expr_node)
        return node

    def visitCaseStatement(self, ctx):
        node = self._create_node(ctx, "caseStatement", text_override=f"CASE {ctx.expression().getText()} OF...")
        node.attributes["selector"] = ctx.expression().getText().strip()
        node.add_child(self.visit(ctx.expression()))
        
        # Handle case labels properly
        if ctx.caseLabelList():
            for case_label_ctx in ctx.caseLabelList().caseLabel():
                label_node = ASTNode("caseLabel", case_label_ctx.getText().strip(), line_number=self._get_original_line(case_label_ctx))
                if case_label_ctx.expression():
                    label_node.attributes["value"] = case_label_ctx.expression().getText().strip()
                    label_node.add_child(self.visit(case_label_ctx.expression()))
                if case_label_ctx.statement():
                    label_node.add_child(self.visit(case_label_ctx.statement()))
                node.add_child(label_node)
        
        if ctx.END_KW():
            node.add_child(self._create_node(ctx.END_KW().getSymbol(), "EndCaseMarker"))
        
        return node

    def visitDropStatement(self, ctx):
        node = self._create_node(ctx, "dropStatement", text_override=ctx.getText().strip())
        node.attributes["identifier"] = self.clean_tal_identifier(ctx.IDENTIFIER().getText())
        return node

    def visitGotoStatement(self, ctx):
        node = self._create_node(ctx, "gotoStatement", text_override=ctx.getText().strip())
        node.attributes["label"] = self.clean_tal_identifier(ctx.IDENTIFIER().getText())
        return node

    def visitScanStatement(self, ctx):
        node = self._create_node(ctx, "scanStatement", text_override=ctx.getText().strip())
        
        # Handle the expressions
        expressions = ctx.expression()
        if len(expressions) >= 2:
            node.attributes["target"] = expressions[0].getText().strip()
            node.attributes["condition"] = expressions[1].getText().strip()
            node.add_child(self.visit(expressions[0]))
            node.add_child(self.visit(expressions[1]))
        
        # Handle the pointer reference
        if ctx.IDENTIFIER():
            node.attributes["pointer"] = self.clean_tal_identifier(ctx.IDENTIFIER().getText())
        
        return node

    def visitRscanStatement(self, ctx):
        node = self._create_node(ctx, "rscanStatement", text_override=ctx.getText().strip())
        
        # Handle the expressions  
        expressions = ctx.expression()
        if len(expressions) >= 2:
            node.attributes["target"] = expressions[0].getText().strip()
            node.attributes["condition"] = expressions[1].getText().strip()
            node.add_child(self.visit(expressions[0]))
            node.add_child(self.visit(expressions[1]))
        
        # Handle the pointer reference
        if ctx.IDENTIFIER():
            node.attributes["pointer"] = self.clean_tal_identifier(ctx.IDENTIFIER().getText())
        
        return node

    def visitStoreStatement(self, ctx):
        node = self._create_node(ctx, "storeStatement", text_override=ctx.getText().strip())
        node.attributes["target"] = ctx.lvalue().getText().strip()
        node.add_child(self.visit(ctx.lvalue()))
        return node

    def visitUseStatement(self, ctx):
        node = self._create_node(ctx, "useStatement", text_override=ctx.getText().strip())
        node.attributes["identifier"] = self.clean_tal_identifier(ctx.IDENTIFIER().getText())
        return node

    def visitExpression(self, ctx):
        return self.visit(ctx.logicalOrExpression())

    def visitLogicalOrExpression(self, ctx):
        if not ctx.OR_OP():
            return self.visit(ctx.logicalAndExpression(0))
        
        node = self._create_node(ctx, "logicalExpression", text_override=ctx.getText().strip())
        node.attributes["operator"] = "OR"
        node.add_child(self.visit(ctx.logicalAndExpression(0)))
        
        for i in range(1, len(ctx.logicalAndExpression())):
            node.add_child(self.visit(ctx.logicalAndExpression(i)))
        
        return node

    def visitRelationalExpression(self, ctx):
        if len(ctx.additiveExpression()) == 1:
            return self.visit(ctx.additiveExpression(0))
        
        node = self._create_node(ctx, "relationalExpression", text_override=ctx.getText().strip())
        
        op_text = ""
        if ctx.EQ(): op_text = "="
        elif ctx.NEQ(): op_text = "<>"
        elif ctx.LT(): op_text = "<"
        elif ctx.GT(): op_text = ">"
        elif ctx.LTE(): op_text = "<="
        elif ctx.GTE(): op_text = ">="
        
        node.attributes["operator"] = op_text
        node.add_child(self.visit(ctx.additiveExpression(0)))
        node.add_child(self.visit(ctx.additiveExpression(1)))
        
        return node

    def visitPrimaryExpression(self, ctx):
        if ctx.literal():
            return self.visit(ctx.literal())
        
        if ctx.IDENTIFIER():
            base_node = self._create_node(ctx.IDENTIFIER().getSymbol(), "Identifier")
            return base_node

        if ctx.qualifiedName():
            return self.visit(ctx.qualifiedName())
        
        if ctx.functionCall():
            return self.visit(ctx.functionCall())
        
        if ctx.LPAREN() and ctx.expression() and ctx.RPAREN():
            return self.visit(ctx.expression())
        
        return self._create_node(ctx, "PrimaryElement", text_override=ctx.getText().strip())

    def visitQualifiedName(self, ctx):
        node = self._create_node(ctx, "qualifiedName", text_override=ctx.getText().strip())
        node.attributes["has_indirection"] = bool(ctx.indirectionSpecifier())
        
        components = [self.clean_tal_identifier(ident.getText()) for ident in ctx.IDENTIFIER()]
        node.attributes["components"] = components
        
        for ident in ctx.IDENTIFIER():
            component_node = self._create_node(ident.getSymbol(), "Identifier")
            node.add_child(component_node)
        
        return node

    def visitFunctionCall(self, ctx):
        node = self._create_node(ctx, "functionCall", text_override=ctx.getText().strip())
        node.attributes["function_name"] = self.clean_tal_identifier(ctx.IDENTIFIER().getText())
        
        if ctx.actualParameterList():
            for expr_ctx in ctx.actualParameterList().expression():
                param_node = self.visit(expr_ctx)
                if param_node:
                    node.add_child(param_node)
        
        return node

    def visitLiteral(self, ctx):
        text = ctx.getText()
        line = self._get_original_line(ctx)
        node = None
        
        if ctx.INT_LITERAL():
            node = ASTNode("intLiteral", text, line_number=line)
            try:
                node.attributes["value"] = int(text.rstrip('D').rstrip('d'))
            except ValueError:
                node.attributes["value"] = text
        elif ctx.STRING_LITERAL():
            node = ASTNode("stringLiteral", text, line_number=line)
            node.attributes["value"] = text[1:-1].replace('""', '"')
        else:
            node = ASTNode("UnknownLiteral", text, line_number=line)
        
        return node
    
    def visitErrorNode(self, node):
        """Handle error nodes gracefully"""
        error_node = ASTNode("ErrorNode", node.getText(), line_number=self._get_original_line(node))
        error_node.attributes["error"] = "Parse error encountered"
        return error_node

class TALASTGenerator:
    def __init__(self):
        self.preprocessor = TALPreprocessor()

    def clean_tal_identifier(self, identifier):
        if not identifier:
            return identifier
        return identifier.replace('^', '_')

    def parse_line_with_antlr_and_visit(self, line_text, original_line_number):
        try:
            class LineErrorListener(ErrorListener):
                def __init__(self, base_line_num):
                    super().__init__()
                    self.errors = []
                    self.base_line_num = base_line_num
                
                def syntaxError(self, recognizer, offendingSymbol, line, column, msg, e):
                    actual_err_line = self.base_line_num + (line - 1)
                    self.errors.append({"line": actual_err_line, "column": column, "message": msg})

            input_stream = InputStream(line_text + '\n')
            lexer = TALLexer(input_stream)
            lexer.removeErrorListeners()
            
            stream = CommonTokenStream(lexer)
            parser = TALParser(stream)
            parser.removeErrorListeners()
            
            error_listener = LineErrorListener(original_line_number)
            parser.addErrorListener(error_listener)
            
            tree = parser.programElement()

            if error_listener.errors:
                return None, error_listener.errors
            
            visitor = CustomTALVisitor(line_number_offset=original_line_number - 1)
            ast_node_from_visitor = visitor.visit(tree)
            
            return ast_node_from_visitor, []

        except Exception as e:
            return None, [{"line": original_line_number, "column": 0, "message": f"ANTLR/Visitor processing error: {str(e)}"}]

    def generate_hybrid_ast(self, tal_source):
        raw_lines = tal_source.split('\n')
        preprocessed_output_with_line_info = self.preprocessor.preprocess_lines(raw_lines)

        root = ASTNode("Program", "TAL_Program")
        errors = []
        
        total_meaningful_original_lines = sum(1 for r_line in raw_lines if r_line.strip() and not r_line.strip().startswith('!'))
        coverage_processed_units = 0

        # Handle comments
        comment_lines_processed = set()

        for r_line_idx, r_line_text in enumerate(raw_lines):
            r_line_actual_num = r_line_idx + 1
            if r_line_text.strip().startswith('!'):
                root.add_child(ASTNode("Comment", r_line_text.strip()[1:], line_number=r_line_actual_num))
                comment_lines_processed.add(r_line_actual_num)

        current_original_line_idx_processed_until = 0

        for p_item, original_start_line_1_based, num_original_lines_consumed in preprocessed_output_with_line_info:
            if (original_start_line_1_based - 1) < current_original_line_idx_processed_until:
                continue 

            if isinstance(p_item, ASTNode):
                if p_item.node_type == "Comment":
                    current_original_line_idx_processed_until = original_start_line_1_based - 1 + num_original_lines_consumed
                    continue 
                else:
                    root.add_child(p_item)
                    coverage_processed_units += 1
                
                current_original_line_idx_processed_until = original_start_line_1_based - 1 + num_original_lines_consumed

            elif isinstance(p_item, str):
                line_to_parse = p_item
                
                if line_to_parse.strip() and not line_to_parse.strip().startswith('!'):
                    antlr_ast_node, antlr_errors = self.parse_line_with_antlr_and_visit(line_to_parse, original_start_line_1_based)
                    if antlr_ast_node and not antlr_errors:
                        root.add_child(antlr_ast_node)
                        coverage_processed_units += 1
                    else:
                        manual_node = self.parse_single_line_manually(line_to_parse, original_start_line_1_based)
                        if manual_node:
                            root.add_child(manual_node)
                            coverage_processed_units += 1
                        else:
                            if antlr_errors:
                                errors.extend(antlr_errors)
                            else:
                                errors.append({"line": original_start_line_1_based, "column": 0, "message": f"Line unparsed: {line_to_parse[:30]}"})
                            root.add_child(ASTNode("UnparsedLine", line_to_parse.strip(), line_number=original_start_line_1_based))
                
                current_original_line_idx_processed_until = original_start_line_1_based - 1 + num_original_lines_consumed

        coverage = (coverage_processed_units / total_meaningful_original_lines * 100.0) if total_meaningful_original_lines > 0 else 0.0
        coverage = min(coverage, 100.0)

        return {
            "source": "HybridWithPreProcessorAndVisitor",
            "success": len(errors) == 0,
            "errors": errors,
            "ast": root.to_dict(),
            "coverage": coverage
        }

    def parse_single_line_manually(self, line_text, line_number_1based):
        stripped_line = line_text.strip()
        if not stripped_line or stripped_line.startswith('!'):
            return None

        if stripped_line.upper().startswith('CALL'):
            return self._parse_call_manually(stripped_line, line_number_1based)
        elif stripped_line.upper().startswith('LITERAL'):
            return self._parse_literal_manually(stripped_line, line_number_1based)
        elif stripped_line.upper().startswith(('INT', 'STRING', 'FIXED', 'REAL')):
            return self._parse_variable_manually(stripped_line, line_number_1based)
        elif stripped_line.startswith('?'):
            return self._parse_directive_manually(stripped_line, line_number_1based)
        elif stripped_line.upper().startswith('RETURN'):
            return ASTNode("returnStatement", stripped_line, line_number=line_number_1based)
        elif stripped_line.upper().startswith('BEGIN'):
            return ASTNode("beginBlock", stripped_line, line_number=line_number_1based)
        elif stripped_line.upper().startswith(('ENDIF', 'ENDWHILE', 'ENDSTRUCT', 'ENDBLOCK')) or stripped_line.upper() == 'END;':
            return ASTNode("endStatement", stripped_line, line_number=line_number_1based)
        elif stripped_line.upper().startswith('ELSE'):
            return ASTNode("elseStatement", stripped_line, line_number=line_number_1based)
        elif ':=' in stripped_line:
            return ASTNode("assignmentStatement", stripped_line, line_number=line_number_1based)
        # NEW: Handle new statements
        elif stripped_line.upper().startswith('ASSERT'):
            return self._parse_assert_manually(stripped_line, line_number_1based)
        elif stripped_line.upper().startswith('CASE'):
            return self._parse_case_manually(stripped_line, line_number_1based)
        elif stripped_line.upper().startswith('DROP'):
            return self._parse_drop_manually(stripped_line, line_number_1based)
        elif stripped_line.upper().startswith('GOTO'):
            return self._parse_goto_manually(stripped_line, line_number_1based)
        elif stripped_line.upper().startswith('RSCAN'):
            return self._parse_rscan_manually(stripped_line, line_number_1based)
        elif stripped_line.upper().startswith('SCAN'):
            return self._parse_scan_manually(stripped_line, line_number_1based)
        elif stripped_line.upper().startswith('STORE'):
            return self._parse_store_manually(stripped_line, line_number_1based)
        elif stripped_line.upper().startswith('USE'):
            return self._parse_use_manually(stripped_line, line_number_1based)
        
        return ASTNode("Statement", self.clean_tal_identifier(stripped_line), line_number=line_number_1based)

    def _parse_call_manually(self, line, line_number):
        call_match = re.match(r'CALL\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*(?:\(([^)]*)\))?\s*;?', line, re.IGNORECASE)
        if call_match:
            func_name = self.clean_tal_identifier(call_match.group(1))
            params_str = call_match.group(2) if call_match.group(2) else ""
            
            node = ASTNode("callStatement", func_name, line_number=line_number)
            node.attributes["function"] = func_name
            node.attributes["call_text"] = line
            node.attributes["parameters"] = []
            
            if params_str:
                params = [p.strip() for p in params_str.split(',')]
                for param in params:
                    param_node = ASTNode("Identifier", self.clean_tal_identifier(param), line_number=line_number)
                    node.add_child(param_node)
                    node.attributes["parameters"].append({"node_type": "Identifier", "text": param.strip()})
            
            return node
        return None

    def _parse_literal_manually(self, line, line_number):
        literal_match = re.match(r'LITERAL\s+(.+)', line, re.IGNORECASE)
        if literal_match:
            node = ASTNode("literalDeclaration", line.strip(), line_number=line_number)
            literal_text = literal_match.group(1).rstrip(';')
            
            items = []
            item_parts = literal_text.split(',')
            for part in item_parts:
                if '=' in part:
                    name, value = part.split('=', 1)
                    items.append({
                        "name": self.clean_tal_identifier(name.strip()),
                        "value": value.strip()
                    })
            
            node.attributes["items"] = items
            return node
        return None

    def _parse_variable_manually(self, line, line_number):
        var_match = re.match(r'(INT|STRING|FIXED|REAL)\s+(.+)', line, re.IGNORECASE)
        if var_match:
            type_name = var_match.group(1).upper()
            vars_text = var_match.group(2).rstrip(';')
            
            node = ASTNode("variableDeclaration", line.strip(), line_number=line_number)
            node.attributes["type"] = type_name.lower()
            
            variables = []
            var_parts = vars_text.split(',')
            for var_part in var_parts:
                var_part = var_part.strip()
                indirection = var_part.startswith('.')
                if indirection:
                    var_part = var_part[1:]
                
                var_name = var_part.split('[')[0].split(':=')[0].strip()
                var_info = {
                    "name": self.clean_tal_identifier(var_name),
                    "indirection": indirection
                }
                
                if '[' in var_part and ']' in var_part:
                    array_start = var_part.find('[')
                    array_end = var_part.find(']') + 1
                    var_info["array_spec"] = var_part[array_start:array_end]
                
                if ':=' in var_part:
                    init_value = var_part.split(':=', 1)[1].strip()
                    var_info["init_value"] = init_value
                
                variables.append(var_info)
            
            node.attributes["variables"] = variables
            return node
        return None

    def _parse_directive_manually(self, line, line_number):
        directive_match = re.match(r'\?([a-zA-Z]+)(.*)', line, re.IGNORECASE)
        if directive_match:
            directive_name = directive_match.group(1)
            directive_params = directive_match.group(2).strip()
            
            node = ASTNode("directiveLine", line.strip(), line_number=line_number)
            node.attributes["directives"] = [f"{directive_name} {directive_params}".strip()]
            return node
        return None

    # NEW: Manual parsing methods for new statements
    def _parse_assert_manually(self, line, line_number):
        assert_match = re.match(r'ASSERT\s+(.+?)\s*;?', line, re.IGNORECASE)
        if assert_match:
            node = ASTNode("assertStatement", line.strip(), line_number=line_number)
            node.attributes["condition"] = self.clean_tal_identifier(assert_match.group(1).strip())
            return node
        return None

    def _parse_case_manually(self, line, line_number):
        # Handle single-line case (unlikely in real TAL)
        case_match = re.match(r'CASE\s+(.+?)\s+OF\s*', line, re.IGNORECASE)
        if case_match:
            node = ASTNode("caseStatement", line.strip(), line_number=line_number)
            node.attributes["selector"] = self.clean_tal_identifier(case_match.group(1).strip())
            # Mark as multi-line construct
            node.attributes["multiline"] = True
            return node
        return None

    def _parse_drop_manually(self, line, line_number):
        drop_match = re.match(r'DROP\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*;?', line, re.IGNORECASE)
        if drop_match:
            node = ASTNode("dropStatement", line.strip(), line_number=line_number)
            node.attributes["identifier"] = self.clean_tal_identifier(drop_match.group(1))
            return node
        return None

    def _parse_goto_manually(self, line, line_number):
        goto_match = re.match(r'GOTO\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*;?', line, re.IGNORECASE)
        if goto_match:
            node = ASTNode("gotoStatement", line.strip(), line_number=line_number)
            node.attributes["label"] = self.clean_tal_identifier(goto_match.group(1))
            return node
        return None

    def _parse_scan_manually(self, line, line_number):
        # Pattern: scan cw[1] while " " -> @begin_ptr;
        scan_match = re.match(r'scan\s+(.+?)\s+while\s+(.+?)\s*->\s*(.+?)\s*;?', line, re.IGNORECASE)
        if scan_match:
            node = ASTNode("scanStatement", line.strip(), line_number=line_number)
            node.attributes["target"] = self.clean_tal_identifier(scan_match.group(1).strip())
            node.attributes["condition"] = scan_match.group(2).strip()
            node.attributes["pointer"] = self.clean_tal_identifier(scan_match.group(3).strip())
            return node
        return None

    def _parse_rscan_manually(self, line, line_number):
        # Pattern: rscan cw[$len(codeword_def.codewrd)] while " " -> @end_ptr;
        rscan_match = re.match(r'rscan\s+(.+?)\s+while\s+(.+?)\s*->\s*(.+?)\s*;?', line, re.IGNORECASE)
        if rscan_match:
            node = ASTNode("rscanStatement", line.strip(), line_number=line_number)
            node.attributes["target"] = self.clean_tal_identifier(rscan_match.group(1).strip())
            node.attributes["condition"] = rscan_match.group(2).strip()
            node.attributes["pointer"] = self.clean_tal_identifier(rscan_match.group(3).strip())
            return node
        return None

    def _parse_store_manually(self, line, line_number):
        store_match = re.match(r'STORE\s+(.+?)\s*;?', line, re.IGNORECASE)
        if store_match:
            node = ASTNode("storeStatement", line.strip(), line_number=line_number)
            node.attributes["target"] = self.clean_tal_identifier(store_match.group(1).strip())
            return node
        return None

    def _parse_use_manually(self, line, line_number):
        use_match = re.match(r'USE\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*;?', line, re.IGNORECASE)
        if use_match:
            node = ASTNode("useStatement", line.strip(), line_number=line_number)
            node.attributes["identifier"] = self.clean_tal_identifier(use_match.group(1))
            return node
        return None

    def save_ast_for_llm(self, ast_result, output_file):
        llm_payload = {
            "instruction": "Convert TAL AST to pseudocode",
            "tal_ast": ast_result,
            "output_format": "structured_pseudocode",
            "requirements": [
                "Convert TAL identifiers (^ to _)",
                "Use standard control flow (IF/THEN/ELSE/END IF)",
                "PROC as PROCEDURE, SUBPROC as SUBPROCEDURE",
                "CALL statements to function calls",
                "Preserve nesting",
                "Add comments for unclear sections"
            ]
        }
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(llm_payload, f, indent=4)
        
        return llm_payload

    def manual_transpile(self, tal_source, target_language="pseudocode"):
        lines = tal_source.strip().split('\n')
        output = []
        indent_level = 0
        block_stack = []
        
        def add_line(line):
            if line.strip():
                output.append("  " * indent_level + line)
            else:
                output.append("")
        
        def clean_id(identifier):
            return identifier.replace('^', '_') if identifier else identifier
        
        add_line(f"// Manually Transpiled from TAL to {target_language}")
        add_line("PROGRAM GeneratedProgram")
        add_line("")
        
        current_line_idx = 0
        while current_line_idx < len(lines):
            line = lines[current_line_idx].strip()
            current_line_idx += 1
            
            if not line or line.startswith('!'):
                if line.startswith('!'):
                    add_line(f"// {line[1:].strip()}")
                else:
                    add_line("")
                continue

            if line.startswith('?'):
                add_line(f"// Directive: {line[1:]}")
                continue
            
            # Block declarations
            if re.match(r'BLOCK\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*;?', line, re.IGNORECASE):
                m = re.match(r'BLOCK\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*;?', line, re.IGNORECASE)
                add_line(f"BLOCK {clean_id(m.group(1))}:")
                indent_level += 1
                block_stack.append("BLOCK")
                
            # Procedure declarations
            elif re.match(r'(PROC|SUBPROC)\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*(?:\(([^)]*)\))?\s*;?', line, re.IGNORECASE):
                m = re.match(r'(PROC|SUBPROC)\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*(?:\(([^)]*)\))?\s*;?', line, re.IGNORECASE)
                proc_type, proc_name, params = m.groups()
                procedure_keyword = "PROCEDURE" if proc_type.upper() == "PROC" else "SUBPROCEDURE"
                param_str = clean_id(params) if params else ""
                add_line(f"{procedure_keyword} {clean_id(proc_name)}({param_str}):")
                indent_level += 1
                block_stack.append(proc_type.upper())
                
            # Literal declarations
            elif re.match(r'LITERAL\s+(.+)', line, re.IGNORECASE):
                m = re.match(r'LITERAL\s+(.+)', line, re.IGNORECASE)
                literal_text = m.group(1).rstrip(';')
                items = self._split_literal_items(literal_text)
                for item in items:
                    if '=' in item:
                        name, value = item.split('=', 1)
                        add_line(f"CONST {clean_id(name.strip())} = {value.strip()}")
                        
            # Variable declarations
            elif re.match(r'(INT|STRING|FIXED|REAL)\s+', line, re.IGNORECASE):
                var_info = self._parse_variable_declaration_simple(line)
                if var_info:
                    for var in var_info["variables"]:
                        var_name = var['name']
                        prefix = "POINTER TO " if var.get('indirection') else ""
                        array_suffix = f" ARRAY{var['array_spec']}" if var.get('array_spec') else ""
                        init_suffix = f" := {var['init_value']}" if var.get('init_value') else ""
                        add_line(f"DECLARE {var_name}: {prefix}{var_info['type'].upper()}{array_suffix}{init_suffix}")
                        
            # Struct declarations
            elif re.match(r'STRUCT\s+(\.?)([a-zA-Z^][a-zA-Z0-9^_]*)\s*;?', line, re.IGNORECASE):
                m = re.match(r'STRUCT\s+(\.?)([a-zA-Z^][a-zA-Z0-9^_]*)\s*;?', line, re.IGNORECASE)
                indirection = m.group(1) == '.'
                struct_name = clean_id(m.group(2))
                prefix = "POINTER TO " if indirection else ""
                add_line(f"{prefix}STRUCT {struct_name}:")
                indent_level += 1
                block_stack.append("STRUCT")
                
            # Begin statements
            elif line.upper() == 'BEGIN':
                add_line("BEGIN")
                indent_level += 1
                block_stack.append("BEGIN_GENERAL")
                
            # End statements
            elif line.upper().startswith('END'):
                indent_level = max(0, indent_level - 1)
                ended = block_stack.pop() if block_stack else "UNKNOWN_BLOCK"
                comment_suffix = f" // {line.split('!', 1)[1].strip()}" if '!' in line else ""
                
                if ended == "BLOCK":
                    add_line(f"END BLOCK{comment_suffix}")
                elif ended == "STRUCT":
                    add_line(f"END STRUCT{comment_suffix}")
                elif ended in ["PROC", "SUBPROC"]:
                    add_line(f"END {ended}{comment_suffix}")
                elif ended == "BEGIN_GENERAL":
                    add_line(f"END{comment_suffix}")
                elif line.upper().startswith("ENDIF"):
                    add_line(f"END IF{comment_suffix}")
                elif line.upper().startswith("ENDWHILE"):
                    add_line(f"END WHILE{comment_suffix}")
                else:
                    add_line(f"END // for {ended}{comment_suffix}")
                    
            # Control flow statements
            elif re.match(r'IF\s+(.+?)\s+THEN', line, re.IGNORECASE):
                m = re.match(r'IF\s+(.+?)\s+THEN', line, re.IGNORECASE)
                condition = self._clean_expression_for_transpile(m.group(1))
                add_line(f"IF {condition} THEN:")
                indent_level += 1
                
            elif re.match(r'ELSE\s+IF\s+(.+?)\s+THEN', line, re.IGNORECASE):
                m = re.match(r'ELSE\s+IF\s+(.+?)\s+THEN', line, re.IGNORECASE)
                condition = self._clean_expression_for_transpile(m.group(1))
                indent_level = max(0, indent_level - 1)
                add_line(f"ELSE IF {condition} THEN:")
                indent_level += 1
                
            elif line.upper() in ['ELSE', 'ELSE;']:
                indent_level = max(0, indent_level - 1)
                add_line("ELSE:")
                indent_level += 1
                
            elif re.match(r'WHILE\s+(.+?)\s+DO', line, re.IGNORECASE):
                m = re.match(r'WHILE\s+(.+?)\s+DO', line, re.IGNORECASE)
                condition = self._clean_expression_for_transpile(m.group(1))
                add_line(f"WHILE {condition} DO:")
                indent_level += 1
                
            # NEW: New statement handling for manual transpilation
            elif re.match(r'ASSERT\s+(.+?)\s*;?', line, re.IGNORECASE):
                m = re.match(r'ASSERT\s+(.+?)\s*;?', line, re.IGNORECASE)
                condition = self._clean_expression_for_transpile(m.group(1))
                add_line(f"ASSERT {condition}")
                
            elif re.match(r'CASE\s+(.+?)\s+OF\s+(.+?)\s+END\s*;?', line, re.IGNORECASE):
                m = re.match(r'CASE\s+(.+?)\s+OF\s+(.+?)\s+END\s*;?', line, re.IGNORECASE)
                selector = self._clean_expression_for_transpile(m.group(1))
                add_line(f"CASE {selector} OF:")
                indent_level += 1
                # Note: Full case label parsing requires multi-line context, so we add a comment
                add_line(f"// Case labels: {m.group(2)}")
                indent_level -= 1
                add_line("END CASE")
                
            elif re.match(r'DROP\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*;?', line, re.IGNORECASE):
                m = re.match(r'DROP\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*;?', line, re.IGNORECASE)
                identifier = clean_id(m.group(1))
                add_line(f"DROP {identifier}")
                
            elif re.match(r'GOTO\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*;?', line, re.IGNORECASE):
                m = re.match(r'GOTO\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*;?', line, re.IGNORECASE)
                label = clean_id(m.group(1))
                add_line(f"GOTO {label}")
                
            elif re.match(r'RSCAN\s+(.+?)\s*,\s*(.+?)\s*;?', line, re.IGNORECASE):
                m = re.match(r'RSCAN\s+(.+?)\s*,\s*(.+?)\s*;?', line, re.IGNORECASE)
                target = self._clean_expression_for_transpile(m.group(1))
                expr = self._clean_expression_for_transpile(m.group(2))
                add_line(f"RSCAN {target}, {expr}")
                
            elif re.match(r'SCAN\s+(.+?)\s*,\s*(.+?)\s*;?', line, re.IGNORECASE):
                m = re.match(r'SCAN\s+(.+?)\s*,\s*(.+?)\s*;?', line, re.IGNORECASE)
                target = self._clean_expression_for_transpile(m.group(1))
                expr = self._clean_expression_for_transpile(m.group(2))
                add_line(f"SCAN {target}, {expr}")
                
            elif re.match(r'STORE\s+(.+?)\s*;?', line, re.IGNORECASE):
                m = re.match(r'STORE\s+(.+?)\s*;?', line, re.IGNORECASE)
                target = self._clean_expression_for_transpile(m.group(1))
                add_line(f"STORE {target}")
                
            elif re.match(r'USE\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*;?', line, re.IGNORECASE):
                m = re.match(r'USE\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*;?', line, re.IGNORECASE)
                identifier = clean_id(m.group(1))
                add_line(f"USE {identifier}")
                
            # Call statements
            elif re.match(r'CALL\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*(?:\(([^)]*)\))?', line, re.IGNORECASE):
                m = re.match(r'CALL\s+([a-zA-Z^][a-zA-Z0-9^_]*)\s*(?:\(([^)]*)\))?', line, re.IGNORECASE)
                func_name = clean_id(m.group(1))
                params_str = m.group(2) if m.group(2) else ""
                
                if params_str:
                    params = [self._clean_expression_for_transpile(p.strip()) for p in params_str.split(',')]
                    param_list = ', '.join(params)
                else:
                    param_list = ""
                
                add_line(f"CALL {func_name}({param_list})")
                
            # Return statements
            elif re.match(r'RETURN(\s+.*?)?;?', line, re.IGNORECASE):
                m = re.match(r'RETURN(\s+.*?)?;?', line, re.IGNORECASE)
                expr = m.group(1)
                if expr and expr.strip():
                    cleaned_expr = self._clean_expression_for_transpile(expr.strip())
                    add_line(f"RETURN {cleaned_expr}")
                else:
                    add_line("RETURN")
                    
            # Assignment statements
            elif ':=' in line:
                var_part, expr_part = line.split(':=', 1)
                var_cleaned = self._clean_expression_for_transpile(var_part.strip())
                expr_cleaned = self._clean_expression_for_transpile(expr_part.rstrip(';').strip())
                add_line(f"{var_cleaned} = {expr_cleaned}")
                
            # Default case
            else:
                cleaned_line = self._clean_expression_for_transpile(line.rstrip(';'))
                if cleaned_line.strip():
                    add_line(f"// Statement: {cleaned_line}")

        add_line("")
        add_line("END PROGRAM")
        return "\n".join(output)

    def _split_literal_items(self, items_text):
        items = []
        current_item = ""
        paren_level = 0
        
        for char in items_text:
            if char == ',' and paren_level == 0:
                if current_item.strip():
                    items.append(current_item.strip())
                current_item = ""
            else:
                current_item += char
                if char == '(':
                    paren_level += 1
                elif char == ')':
                    paren_level -= 1
        
        if current_item.strip():
            items.append(current_item.strip())
        
        return items

    def _parse_variable_declaration_simple(self, line):
        type_match = re.match(r'(INT|STRING|FIXED|REAL)\s+(.+)', line, re.IGNORECASE)
        if not type_match:
            return None
            
        type_name = type_match.group(1).upper()
        vars_text = type_match.group(2).rstrip(';')
        
        variables = []
        var_parts = self._split_variable_declarators(vars_text)
        
        for var_part in var_parts:
            var_info = self._parse_single_variable_simple(var_part.strip())
            if var_info:
                variables.append(var_info)
        
        return {
            "type": type_name.lower(),
            "variables": variables
        }

    def _split_variable_declarators(self, vars_text):
        variables = []
        current_var = ""
        bracket_level = 0
        paren_level = 0
        
        for char in vars_text:
            if char == ',' and bracket_level == 0 and paren_level == 0:
                if current_var.strip():
                    variables.append(current_var.strip())
                current_var = ""
            else:
                current_var += char
                if char == '[':
                    bracket_level += 1
                elif char == ']':
                    bracket_level -= 1
                elif char == '(':
                    paren_level += 1
                elif char == ')':
                    paren_level -= 1
        
        if current_var.strip():
            variables.append(current_var.strip())
        
        return variables

    def _parse_single_variable_simple(self, var_text):
        var_match = re.match(r'(\.?)\s*([a-zA-Z^][a-zA-Z0-9^_]*)\s*(\[[^\]]*\])?\s*(?::=\s*(.+))?', var_text)
        if var_match:
            indirection = var_match.group(1) == '.'
            name = var_match.group(2)
            array_spec = var_match.group(3)
            init_value = var_match.group(4)
            
            var_info = {
                "name": self.clean_tal_identifier(name),
                "indirection": indirection
            }
            
            if array_spec:
                var_info["array_spec"] = array_spec
            if init_value:
                var_info["init_value"] = init_value.strip()
            
            return var_info
        return None

    def _clean_expression_for_transpile(self, expr_str):
        if not expr_str:
            return ""
        
        cleaned = expr_str.strip().rstrip(';')
        in_string = False
        result = []
        
        for char in cleaned:
            if char == '"':
                in_string = not in_string
            result.append('_' if char == '^' and not in_string else char)
        
        return "".join(result)


# Main execution
if __name__ == "__main__":
    if len(sys.argv) < 2 or len(sys.argv) > 4:
        print("Usage: python talTranspiler.py <input_tal_file> [mode] [target_language]")
        print("Modes: ast (default, hybrid Visitor), transpile (manual only), hybrid (AST + manual transpile)")
        print("Target languages: pseudocode (default), java")
        sys.exit(1)
    
    input_file = sys.argv[1]
    mode = sys.argv[2].lower() if len(sys.argv) >= 3 else "ast"
    target_language = sys.argv[3].lower() if len(sys.argv) == 4 else "pseudocode"
    
    if mode not in ["ast", "transpile", "hybrid"]:
        print(f"Warning: Invalid mode '{mode}'. Defaulting to 'ast'.")
        mode = "ast"
    
    if not os.path.exists(input_file):
        print(f"Error: Input file '{input_file}' not found.")
        sys.exit(1)

    try:
        with open(input_file, 'r', encoding='utf-8') as f:
            tal_source = f.read()
        
        print(f"Reading TAL source from: {input_file}")
        print(f"Mode: {mode}")
        if mode in ["transpile", "hybrid"]:
            print(f"Target Language for Transpilation: {target_language}")
        print("=" * 50)
        
        generator = TALASTGenerator()
        
        if mode == "ast" or mode == "hybrid":
            print("Generating AST (Hybrid with Preprocessor & Visitor)...")
            ast_result = generator.generate_hybrid_ast(tal_source)
            
            base_name = input_file[:-4] if input_file.lower().endswith(".tal") else (input_file.rsplit('.', 1)[0] if '.' in input_file else input_file)
            ast_file = base_name + '_ast.json'
            generator.save_ast_for_llm(ast_result, ast_file)
            
            print(f"\nAST saved to: {ast_file}")
            print(f"  Source: {ast_result.get('source', 'N/A')}")
            print(f"  Success: {ast_result.get('success', False)}")
            print(f"  Coverage: {ast_result.get('coverage', 0.0):.1f}%")
            if ast_result.get('errors'):
                print(f"  Errors: {len(ast_result['errors'])}")
                for error in ast_result['errors'][:5]:  # Show first 5 errors
                    print(f"    Line {error.get('line', '?')}: {error.get('message', 'Unknown error')}")
                if len(ast_result['errors']) > 5:
                    print(f"    ... and {len(ast_result['errors']) - 5} more errors")

            if mode == "hybrid":
                print("\nManually Transpiling from original source...")
                transpiled_code = generator.manual_transpile(tal_source, target_language)
                code_file_suffix = '.java' if target_language == 'java' else '_pseudocode.txt'
                code_file = base_name + code_file_suffix
                
                with open(code_file, 'w', encoding='utf-8') as f:
                    f.write(f"// Hybrid Mode: AST in {ast_file}\n")
                    f.write(f"// Manual Transpile from source:\n\n")
                    f.write(transpiled_code)
                
                print(f"Manually transpiled code saved to: {code_file}")

        elif mode == "transpile":
            print("Manually Transpiling...")
            result = generator.manual_transpile(tal_source, target_language)
            
            base_name = input_file[:-4] if input_file.lower().endswith(".tal") else (input_file.rsplit('.', 1)[0] if '.' in input_file else input_file)
            output_file_suffix = '.java' if target_language == 'java' else '_pseudocode.txt'
            output_file = base_name + output_file_suffix
            
            with open(output_file, 'w', encoding='utf-8') as f:
                f.write(result)
            
            print(f"Manually transpiled code saved to: {output_file}")
            
    except (FileNotFoundError, ImportError) as e:
        if any(module in str(e) for module in ['TALLexer', 'TALParser', 'TALVisitor']):
            print(f"Error: ANTLR generated files not found or import failed: {e}")
            print("Please generate them from TAL.g4 using:")
            print("  antlr4 -Dlanguage=Python3 TAL.g4 -visitor -no-listener")
        else:
            print(f"File/Import Error: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"An unexpected error occurred: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

