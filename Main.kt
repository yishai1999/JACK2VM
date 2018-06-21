// Dvir Schnaps 208299453
// Yishai Jaffe 207612920
// Targil 5

import java.io.*


fun main(args: Array<String>) {
	val input = readLine()
	var dir = File(input)
	dir.listFiles().forEach{it ->
		if (it.extension == "jack"){
			parsingEngine(it)
		}
	}
	println("All done, man :)")
}

fun parsingEngine (jFile: File) {
	var xmlT: Tokenizer
	var xmlP: Parser
	xmlT = Tokenizer(jFile)
	xmlP = Parser(xmlT.tokenize())
	xmlP.parseClass()
}


