#!/bin/bash
# build.sh - Build script for TAL parser

rm *.class
rm -fr __pycache__
rm -f *.class *.tokens TALL*.* TAL.interp TAL.tokens


# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Building TAL Parser...${NC}"

# Check if ANTLR jar exists
ANTLR_JAR="antlr-4.13.2-complete.jar"
if [ ! -f "$ANTLR_JAR" ]; then
    echo -e "${RED}Error: $ANTLR_JAR not found!${NC}"
    echo "Download it from: https://www.antlr.org/download.html"
    exit 1
fi

# Set classpath
export CLASSPATH=".:$ANTLR_JAR:$CLASSPATH"

echo -e "${YELLOW}Step 1: Generating ANTLR parser files...${NC}"
java -jar $ANTLR_JAR -Dlanguage=Python3 -visitor TAL.g4
if [ $? -ne 0 ]; then
    echo -e "${RED}Error: ANTLR generation failed!${NC}"
    exit 1
fi


echo -e "${GREEN}Build successful!${NC}"
echo ""
echo "Usage examples:"
echo "  python3 talTranspiler.py <input tal> ast"
#echo "  java -cp \".:$ANTLR_JAR\" TALParserMain sample.tal"
#echo "  java -cp \".:$ANTLR_JAR\" TALParserMain -gui sample.tal"
#echo "  java -cp \".:$ANTLR_JAR\" TALParserMain -tree -tokens sample.tal"

# Create sample TAL file for testing
cat > sample.tal << 'EOF'
?page "<><><><> GLOBALS <><><><>"

?nolist
?source =IFT3TAL_LIBGLOBL(globals)
?list

BLOCK MY_GLOBALS;

literal rcv^len = 1024,
        ready   = 0,
        stopped = 1,
        active  = 2;

int rcv^file := -1,
    .rcv^buf[0:rcv^len/2],
    rdcnt,
    status;

STRUCT .fed^msg;
BEGIN
    struct hdr;
    begin string byte[0:16]; end;
    struct msg;
    begin string byte[0:1024]; end;
END;

END; ! END OF BLOCK MY_GLOBALS

PROC test^procedure(param1);
string .param1;
BEGIN
    INT x := 10;
    STRING name := "Test";
    
    IF x > 5 THEN
        CALL numout(name, x, 10, 2);
    ENDIF;
    
    WHILE x > 0 DO
        x := x - 1;
    ENDWHILE;
    
    RETURN x;
END;

PROC send^message(message^len);
INT message^len;
BEGIN
    ! Process message
    blank^fill(rcv^buf);
    
    IF message^len > 0 THEN
        call writeupdateunlock(file^handle, buffer, message^len);
    ENDIF;
END;
EOF

echo -e "${GREEN}Created sample.tal for testing${NC}"

# Create run script
cat > run.sh << 'EOF'
#!/bin/bash
# Convenience script to run TAL parser

ANTLR_JAR="antlr-4.13.2-complete.jar"
export CLASSPATH=".:$ANTLR_JAR:$CLASSPATH"

if [ $# -eq 0 ]; then
    echo "Usage: ./run.sh [options] <tal-file>"
    echo "Options:"
    echo "  -gui      Show parse tree in GUI"
    echo "  -tree     Print parse tree"
    echo "  -tokens   Show token stream"
    echo "  -verbose  Verbose output"
    echo ""
    echo "Examples:"
    echo "  ./run.sh sample.tal"
    echo "  ./run.sh -gui sample.tal"
    echo "  ./run.sh -tree -tokens sample.tal"
else
	python3 talTranspiler.py "$@"
    #java -cp ".:$ANTLR_JAR" TALParserMain "$@"
fi
EOF

chmod +x run.sh

echo -e "${GREEN}Created run.sh convenience script${NC}"
echo ""
echo "Quick test:"
echo "  ./run.sh sample.tal"

