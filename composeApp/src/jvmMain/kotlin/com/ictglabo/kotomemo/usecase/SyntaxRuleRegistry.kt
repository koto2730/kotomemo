package com.ictglabo.kotomemo.usecase

import java.nio.file.Path
import java.util.Locale

object SyntaxRuleRegistry {

    private val cFamily = SyntaxRuleSet(
        name = "c-family",
        rules = listOf(
            SyntaxRule(TokenKind.Comment, Regex("""//[^\n]*""")),
            SyntaxRule(TokenKind.Comment, Regex("""/\*[\s\S]*?\*/""")),
            SyntaxRule(TokenKind.StringLit, Regex(""""(?:\\.|[^"\\\n])*"""")),
            SyntaxRule(TokenKind.StringLit, Regex("""'(?:\\.|[^'\\\n])*'""")),
        ),
    )

    private val hashFamily = SyntaxRuleSet(
        name = "hash",
        rules = listOf(
            SyntaxRule(TokenKind.Comment, Regex("""#[^\n]*""")),
            SyntaxRule(TokenKind.StringLit, Regex(""""(?:\\.|[^"\\\n])*"""")),
            SyntaxRule(TokenKind.StringLit, Regex("""'(?:\\.|[^'\\\n])*'""")),
        ),
    )

    private val xmlFamily = SyntaxRuleSet(
        name = "xml",
        rules = listOf(
            SyntaxRule(TokenKind.Comment, Regex("""<!--[\s\S]*?-->""")),
            SyntaxRule(TokenKind.StringLit, Regex(""""(?:\\.|[^"\\\n])*"""")),
            SyntaxRule(TokenKind.StringLit, Regex("""'(?:\\.|[^'\\\n])*'""")),
        ),
    )

    private val sqlFamily = SyntaxRuleSet(
        name = "sql-dash",
        rules = listOf(
            SyntaxRule(TokenKind.Comment, Regex("""--[^\n]*""")),
            SyntaxRule(TokenKind.Comment, Regex("""/\*[\s\S]*?\*/""")),
            SyntaxRule(TokenKind.StringLit, Regex("""'(?:''|[^'\n])*'""")),
        ),
    )

    private val byExtension: Map<String, SyntaxRuleSet> = buildMap {
        listOf("kt", "kts", "java", "js", "mjs", "ts", "tsx", "jsx", "c", "cc", "cpp",
            "cxx", "h", "hpp", "cs", "go", "rs", "swift", "scala", "dart", "css", "scss",
            "less", "json", "json5", "groovy", "gradle").forEach { put(it, cFamily) }
        listOf("py", "pyw", "sh", "bash", "zsh", "rb", "yml", "yaml", "toml",
            "conf", "ini", "cfg", "env", "tf", "pl", "r").forEach { put(it, hashFamily) }
        listOf("html", "htm", "xml", "svg", "xhtml", "xsd", "xsl").forEach { put(it, xmlFamily) }
        listOf("sql", "lua", "hs", "ex", "exs").forEach { put(it, sqlFamily) }
    }

    private val byBasename: Map<String, SyntaxRuleSet> = mapOf(
        "Makefile" to hashFamily,
        "Dockerfile" to hashFamily,
        ".gitignore" to hashFamily,
        ".gitattributes" to hashFamily,
    )

    fun rulesFor(path: Path?): SyntaxRuleSet? {
        if (path == null) return null
        val name = path.fileName?.toString() ?: return null
        byBasename[name]?.let { return it }
        val ext = name.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.ROOT)
        if (ext.isEmpty()) return null
        return byExtension[ext]
    }
}
