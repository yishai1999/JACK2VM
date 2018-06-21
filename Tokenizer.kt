import java.io.*

class Tokenizer (jFile: File) {
 	val xml = File(jFile.invariantSeparatorsPath.dropLast(5) + "T.xml")
 	val file = jFile
 	val symbols = "{}()[].,;+-/*&|<>=~".split("").toTypedArray()
 	val keywords = "class constructor function method field static var int char boolean void true false null this let do if else while return".split(" ").toTypedArray()
 	val tokMap = hashMapOf("<" to "&lt;", ">" to "&gt;", "\"" to "&quot;", "&" to "&amp;")
 	init {
 		xml.delete()
 	}

 	fun tokenize(): File {
 		xml.appendText("<tokens>\n")
 		file.forEachLine{it1 ->
				if(!it1.isComment())
					if (!containsStrings(it1.trim()))
						_tokenize(it1.trim())}
	 	xml.appendText("</tokens>")
	 	return xml
 	}

 	fun String.isComment(): Boolean {
 		if (this.trim().startsWith("*")) return true
 		if (this.trim().startsWith("/")) return true
 		if (this.isEmpty()) return true
 		return false
 	}

 	fun containsStrings(line: String): Boolean {
 		var splitLine = line.split("\"")
 		if(splitLine.size == 1) return false
 		for (i in 0..splitLine.size-1) {
 			if (i%2 == 0) _tokenize(splitLine[i])
 			else {
 				xml.appendText("<stringConstant> " + (tokMap[splitLine[i]]?:splitLine[i]) + " </stringConstant>\n")
 			}
 		}
 		return true
 	} 

 	fun _tokenize(line: String) {
 		var tokType: String
	 		line.stripComment().splitKeeping("{","}","(",")","[","]",".",",",";","+","-","/","*","&","|","<",">","=","~"," ","\t").forEach { tok ->
	 			var tok1 = tok
	 			when {
	 				tok in symbols -> tokType = "symbol"
	 				tok in keywords -> tokType = "keyword"
	 				tok.isInt() -> tokType = "integerConstant"
	 				else -> tokType = "identifier"
	 			}
	 			if (tok != " ") {
	 				xml.appendText("<" + tokType + "> " + (tokMap[tok1]?:tok1) + " </" + tokType + ">\n")
	 			}
	 		}
 	}
}

fun String.stripComment(): String {
	return this.split("//")[0]
}

fun String.isInt(): Boolean {
	val v = this.toIntOrNull()
    return when(v) {
        null -> false
        else -> true
    }
}

fun String.splitKeeping(str: String): List<String> {
 	return this.split(str).flatMap {listOf(it, str)}.dropLast(1).filterNot {it1->it1.isEmpty()}
}

fun String.splitKeeping(vararg strs: String): List<String> {
	var res = listOf(this)
	strs.forEach {str -> res  = res.flatMap {it.splitKeeping(str)} }
	return res
}