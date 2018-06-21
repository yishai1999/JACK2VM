import java.io.*

class Symbol(_name: String, _type: String, _kind: String, _index: Int) {
	val name = _name
	val type = _type
	val kind = _kind
	val index = _index
}

class SymbolTable {
	var table = ArrayList<Symbol>()
	fun startSubroutine() {// Erases all variables from previous functions
		var functiontTable = ArrayList<Symbol>() 
		for (sym in table) {
			if (sym.kind == "var" || sym.kind == "argument") functiontTable.add(sym)
		} 
		for (sym in functiontTable) {
			table.remove(sym)
		}
	}
	fun define(name: String, type: String, kind: String) {
		var maxIndex = 0
		for (sym in table) {
			if (sym.kind == kind) maxIndex += 1
		}
		table.add(Symbol(name,type,kind,maxIndex))
	}
	fun kindOf(name: String): String? {
		for (sym in table) {
			if (sym.name == name) return sym.kind
		}
		return null
	}
	fun typeOf(name: String): String? {
		for (sym in table) {
			if (sym.name == name) return sym.type
		}
		return null
	}
	fun indexOf(name: String): Int? {
		for (sym in table) {
			if (sym.name == name) return sym.index
		}
		return null
	}
	fun maxIndex(kind: String): Int {
		var max = 0
		for (sym in table) {
			if (sym.kind == kind) max += 1
		}
		return max
	}
}
class VMWriter(_file: String) {// Interface for writing vm code easily
	val file = File(_file)
	init {
		file.delete()
	}
	fun writePush(segment: String?, index: Int?) {
		file.appendText("push $segment $index\n")
	}
	fun writePop(segment: String?, index: Int?) {
		file.appendText("pop $segment $index\n")
	}
	fun writeArithmetic(command: String?) {
		file.appendText("$command\n")
	}
	fun writeLabel(label: String?) {
		file.appendText("label $label\n")
	}
	fun writeGoto(label: String?) {
		file.appendText("goto $label\n")
	}
	fun writeIf(label: String?) {
		file.appendText("if-goto $label\n")
	}
	fun writeCall(funcName: String?, argNum: Int?) {
		file.appendText("call $funcName $argNum\n")
	}
	fun writeFunction(funcName: String?, locNum: Int?) {
		file.appendText("function $funcName $locNum\n")
	}
	fun writeReturn() {
		file.appendText("return\n")
	}
}