import java.io.*

class Parser (tFile: File) {
	val text = tFile.reader().readLines()
	val xml = File(tFile.invariantSeparatorsPath.dropLast(5)+".xml")
	val symbolTable = SymbolTable()
	val kindMap = hashMapOf("field" to "this", "var" to "local")
	val writer = VMWriter(tFile.invariantSeparatorsPath.dropLast(5)+".vm")
	var cur = 1
	var depth = 0
	var lc = 1
	var className = ""
	init {
 		xml.delete()
 	}

	fun curTok(lookAhead: Int = 0): Array<String> = text[cur+lookAhead].splitTok()
	fun next() { cur += 1 }
	fun writeTerminal() {// Writing the current token as a terminal
		tab()
		xml.appendText("<" + curTok()[0] + "> " + curTok()[1] + " </" + curTok()[0] + ">\n")
		next()
	}
	fun tab() {for (i in 1..depth) xml.appendText("\t")}
	fun openScope(name: String) {tab(); xml.appendText("<" + name + ">\n"); depth += 1}
	fun closeScope(name: String) {depth = depth - 1; tab(); xml.appendText("</" + name + ">\n")}
	fun checkTag(vararg strs: String, _lookAhead: Int = 0) = strs.any {str -> str == curTok(_lookAhead)[0]}
	fun checkLexeme(vararg strs: String, _lookAhead: Int = 0) = strs.any {str -> str == curTok(_lookAhead)[1]}
	fun checkType() = checkLexeme("int","char","boolean") || checkTag("identifier")
	fun matchTag(vararg strs: String, lookAhead: Int = 0): Boolean {
		if (checkTag(*strs,_lookAhead=lookAhead)) {writeTerminal(); return true}
		return false
	}
	fun matchLexeme(vararg strs: String, lookAhead: Int = 0): Boolean {
		if (checkLexeme(*strs,_lookAhead=lookAhead)) {writeTerminal(); return true}
		return false
	}
	fun matchType(): Boolean {
		if (checkType()) {writeTerminal(); return true}
		return false
	}
	fun labelCounter(): Int {
		var temp = lc
		lc += 1
		return temp
	}

	fun parseClass() {
		openScope("class")
		matchLexeme("class")
		className = curTok()[1]
		matchTag("identifier")
		matchLexeme("{")
		while (checkLexeme("static", "field")) {
			parseClassVarDec()
		}
		while (checkLexeme("constructor", "function", "method")) {
			parseSubroutineDec()
		}
		matchLexeme("}")
		closeScope("class")
	}

	fun parseClassVarDec() {
		openScope("classVarDec")
		var kind = curTok()[1]
		matchLexeme("static", "field")
		var type = curTok()[1]
		matchType()
		var name = curTok()[1]
		matchTag("identifier")
		symbolTable.define(name,type,kind)
		while (matchLexeme(",")) {// Add each field to the symbol table
			name = curTok()[1]
			symbolTable.define(name,type,kind)
			matchTag("identifier")
		}
		matchLexeme(";")
		closeScope("classVarDec")
	}

	fun parseSubroutineDec() {
		openScope("subroutineDec")
		var isMethod = false
		symbolTable.startSubroutine()// Clear any variables related to past functions
		if (curTok()[1] == "method") {
			symbolTable.define("this",className,"argument")// Make sure the method will know what "this" is
			isMethod = true
		}
		matchLexeme("constructor", "function", "method")
		matchType()
		matchLexeme("void")
		var funcName = curTok()[1]
		matchTag("identifier")
		matchLexeme("(")
		parseParameterList()
		matchLexeme(")")
		parseSubroutineBody(funcName,isMethod)
		closeScope("subroutineDec")
	}

	fun parseParameterList() {
		openScope("parameterList")
		if (checkType()) {
			var type = curTok()[1]
			matchType()
			var name = curTok()[1]
			matchTag("identifier")
			symbolTable.define(name,type,"argument")
			while (matchLexeme(",")){// Add each argument to the symbo table
				type = curTok()[1]
				matchType()
				name = curTok()[1]
				matchTag("identifier")
				symbolTable.define(name,type,"argument")
			}
		}
		closeScope("parameterList")
	}
	fun parseSubroutineBody(funcName: String,isMethod: Boolean) {
		openScope("subroutineBody")
		var localsCount = 0
		matchLexeme("{")
		while (checkLexeme("var")) {
			localsCount += parseVarDec()
		}
		writer.writeFunction(className + "." + funcName, localsCount)
		if (funcName == "new") {// Allocate room for the creation of an object
			writer.writePush("constant",symbolTable.maxIndex("field"))
			writer.writeCall("Memory.alloc",1)
			writer.writePop("pointer",0)
		}
		if (isMethod) {// Puts the address of "this" in RAM[THIS]
			writer.writePush("argument",0)
			writer.writePop("pointer",0)
		}
		parseStatements()
		matchLexeme("}")
		closeScope("subroutineBody")
	}

	fun parseVarDec(): Int {
		openScope("varDec")
		var localsCount = 1
		matchLexeme("var")
		var type = curTok()[1]
		matchType()
		var name = curTok()[1]
		matchTag("identifier")
		symbolTable.define(name,type,"var")
		while (matchLexeme(",")){// Add each local variable to the symbol table
			name = curTok()[1]
			matchTag("identifier")
			symbolTable.define(name,type,"var")
			localsCount += 1
		}
		matchLexeme(";")
		closeScope("varDec")
		return localsCount
	}

	fun parseStatements() {
		openScope("statements")
		while (checkLexeme("let","if","while","do","return")) {
			parseStatement()
		}
		closeScope("statements")
	}

	fun parseStatement() {
		when {
			checkLexeme("let") -> parseLetStatement()
			checkLexeme("if") -> parseIfStatement()
			checkLexeme("while") -> parseWhileStatement()
			checkLexeme("do") -> parseDoStatement()
			checkLexeme("return") -> parseReturnStatement()
			else -> println("error")
		}
	}

	fun parseLetStatement() {
		openScope("letStatement")
		matchLexeme("let")
		var resSeg = symbolTable.kindOf(curTok()[1])// Get the segment of the variable
		resSeg = kindMap[resSeg]?:resSeg
		var resIndex = symbolTable.indexOf(curTok()[1])// Get the index of the variable
		matchTag("identifier")
		if (matchLexeme("[")) {
			parseExpression()
			writer.writePush(resSeg,resIndex)
			writer.writeArithmetic("add")// If it's an array - calculate the index and add it to the base address
			matchLexeme("]")
			writer.writePop("temp",0)
			matchLexeme("=")
			parseExpression()
			writer.writePush("temp",0)
			writer.writePop("pointer",1)
			writer.writePop("that",0)
		}
		else {
			matchLexeme("=")
			parseExpression()
			writer.writePop(resSeg,resIndex)
		}
		matchLexeme(";")
		closeScope("letStatement")
	}

	fun parseIfStatement() {
		openScope("ifStatement")
		var lc = labelCounter()// Get the current label counter and then increment it
		matchLexeme("if")
		matchLexeme("(")
		parseExpression()
		writer.writeIf("true$lc")
		writer.writeGoto("false$lc")
		writer.writeLabel("true$lc")
		matchLexeme(")")
		matchLexeme("{")
		parseStatements()
		matchLexeme("}")
		if (matchLexeme("else")) {
			writer.writeGoto("endif$lc")
			writer.writeLabel("false$lc")
			matchLexeme("{")
			parseStatements()
			matchLexeme("}")
			writer.writeLabel("endif$lc")
		}
		else {
			writer.writeLabel("false$lc")
		}
		closeScope("ifStatement")
	}

	fun parseWhileStatement() {
		openScope("whileStatement")
		var lc = labelCounter()// Get the current label counter and then increment it
		matchLexeme("while")
		writer.writeLabel("while$lc")
		matchLexeme("(")
		parseExpression()
		matchLexeme(")")
		writer.writeArithmetic("not")
		writer.writeIf("false$lc")
		matchLexeme("{")
		parseStatements()
		matchLexeme("}")
		writer.writeGoto("while$lc")
		writer.writeLabel("false$lc")
		closeScope("whileStatement")
	}

	fun parseDoStatement() {
		openScope("doStatement")
		matchLexeme("do")
		parseSubroutineCall()
		writer.writePop("temp",0)// Get rid of the return value of the void function
		matchLexeme(";")
		closeScope("doStatement")
	}

	fun parseReturnStatement() {
		openScope("returnStatement")
		matchLexeme("return")
		if (!checkLexeme(";")) {
			parseExpression()
		}
		else {
			writer.writePush("constant",0)// A void function returns 0
		}
		matchLexeme(";")
		writer.writeReturn()
		closeScope("returnStatement")
	}

	fun parseExpression() {
		openScope("expression")
		parseTerm()
		while (matchLexeme("+","-","*","/","&amp;","|","&lt;","&gt;","=")) {
			var op = curTok(-1)[1]
			parseTerm()
			when(op) {
				"+" -> writer.writeArithmetic("add")
				"-" -> writer.writeArithmetic("sub")
				"*" -> writer.writeCall("Math.multiply", 2)
				"/" -> writer.writeCall("Math.divide", 2)
				"&amp;" -> writer.writeArithmetic("and")
				"|" -> writer.writeArithmetic("or")
				"&lt;" -> writer.writeArithmetic("lt")
				"&gt;" -> writer.writeArithmetic("gt")
				"=" -> writer.writeArithmetic("eq")
			}
		}
		closeScope("expression")
	}

	fun parseSubroutineCall() {
		var args = 0
		var funcName = curTok()[1]
		matchTag("identifier")
		if (matchLexeme(".")) {
			if (symbolTable.kindOf(funcName)!=null) {// If the prefix is a object then it's a method
				var varSeg = symbolTable.kindOf(funcName)// and we need to send the object as the first argument
				varSeg = kindMap[varSeg]?:varSeg
				var varIndex = symbolTable.indexOf(funcName)
				writer.writePush(varSeg,varIndex)
				args = 1
				funcName = symbolTable.typeOf(funcName)?: funcName// Call "ClassName.funcName"
			}
			funcName += "." + curTok()[1]
			matchTag("identifier")
		}
		else {
			funcName = className + "." + funcName// If there is no prefix then it should be the current class name
			writer.writePush("pointer",0)// The method needs "this" as its first argument
			args = 1
		}
		matchLexeme("(")
		args += parseExpressionList()
		matchLexeme(")")
		writer.writeCall(funcName, args)
	}

	fun parseTerm() {
		openScope("term")
		if (matchTag("integerConstant")) {
			writer.writePush("constant", curTok(-1)[1].toIntOrNull())
		}
		else if (matchTag("stringConstant")) {
			var str = curTok(-1)[1]
			writer.writePush("constant",str.length)
			writer.writeCall("String.new",1)
			for (i in 0..str.length-1) {
				writer.writePush("constant",str[i].toInt())
				writer.writeCall("String.appendChar",2)
			}
		}
		else if (matchLexeme("true","false","null","this")) {
			when (curTok(-1)[1]) {
				"true" -> {
					writer.writePush("constant", 0)
					writer.writeArithmetic("not")
				}
				"false" -> writer.writePush("constant", 0)
				"null" -> writer.writePush("constant", 0)
				"this" -> writer.writePush("pointer", 0)
			}
		}
		else if (matchLexeme("-","~")) {
			var op = curTok(-1)[1]
			parseTerm()
			when (op) {
				"-" -> writer.writeArithmetic("neg")
				"~" -> writer.writeArithmetic("not")
			}
		}
		else if (matchLexeme("(")) {
			parseExpression()
			matchLexeme(")")
		}
		else if (checkLexeme("(",".",_lookAhead = 1)) {// If a subroutine call is involved
			parseSubroutineCall()
		}
		else {
			var resSeg = symbolTable.kindOf(curTok()[1])
			resSeg = kindMap[resSeg]?:resSeg
			var resIndex = symbolTable.indexOf(curTok()[1])
			matchTag("identifier")
			if (matchLexeme("[")) {
				parseExpression()
				writer.writePush(resSeg,resIndex)
				writer.writeArithmetic("add")// If it's an array - calculate the index and add it to the base address
				matchLexeme("]")
				writer.writePop("pointer",1)
				writer.writePush("that",0)
			}
			else {
				writer.writePush(resSeg,resIndex)
			}
		}
		closeScope("term")
	}

	fun parseExpressionList(): Int {
		openScope("expressionList")
		var expCount = 0
		if (!checkLexeme(")")) {
			parseExpression()
			expCount += 1
			while (matchLexeme(",")) {
				parseExpression()
				expCount += 1
			}
		}
		closeScope("expressionList")
		return expCount
	}
}

fun String.splitTok(): Array<String> {
	var lexeme = this.split(" ").dropLast(1).drop(1).joinToString(" ")
	var res = arrayOf(this.split(" ")[0].drop(1).dropLast(1),lexeme)
	return res
}
