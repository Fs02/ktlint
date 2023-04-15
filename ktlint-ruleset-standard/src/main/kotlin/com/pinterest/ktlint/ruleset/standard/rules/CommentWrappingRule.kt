package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RBRACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.firstChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.hasNewLineInClosedRange
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiCommentImpl
import org.jetbrains.kotlin.psi.psiUtil.leaves

/**
 * Checks external wrapping of block comments. Wrapping inside the comment is not altered. A block comment following another element on the
 * same line is replaced with an EOL comment, if possible.
 */
public class CommentWrappingRule :
    StandardRule(
        id = "comment-wrapping",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == BLOCK_COMMENT) {
            val beforeBlockComment =
                node
                    .leaves(false)
                    .takeWhile { it.isWhiteSpace() && !it.textContains('\n') }
                    .firstOrNull()
                    ?: node.firstChildLeafOrSelf()
            val afterBlockComment =
                node
                    .leaves()
                    .takeWhile { it.isWhiteSpace() && !it.textContains('\n') }
                    .firstOrNull()
                    ?: node.lastChildLeafOrSelf()

            if (!node.textContains('\n') &&
                !node.isKtlintSuppressionDirective() &&
                beforeBlockComment.prevLeaf().isWhitespaceWithNewlineOrNull() &&
                afterBlockComment.nextLeaf().isWhitespaceWithNewlineOrNull()
            ) {
                emit(node.startOffset, "A single line block comment must be replaced with an EOL comment", true)
                if (autoCorrect) {
                    node.replaceWithEndOfLineComment()
                }
            }

            if (!beforeBlockComment.prevLeaf().isWhitespaceWithNewlineOrNull() &&
                !afterBlockComment.nextLeaf().isWhitespaceWithNewlineOrNull()
            ) {
                if (hasNewLineInClosedRange(beforeBlockComment, afterBlockComment)) {
                    // Do not try to fix constructs like below:
                    //    val foo = "foo" /*
                    //    some comment
                    //    */ val bar = "bar"
                    emit(
                        node.startOffset,
                        "A block comment starting on same line as another element and ending on another line before another element is " +
                            "disallowed",
                        false,
                    )
                } else if (beforeBlockComment.prevLeaf()?.elementType == LBRACE &&
                    afterBlockComment.nextLeaf()?.elementType == RBRACE
                ) {
                    // Allow single line blocks containing a block comment
                    //   val foo = { /* no-op */ }
                    return
                } else {
                    // Do not try to fix constructs like below:
                    //    val foo /* some comment */ = "foo"
                    emit(node.startOffset, "A block comment in between other elements on the same line is disallowed", false)
                }
                return
            }

            beforeBlockComment
                .prevLeaf()
                .takeIf { !it.isWhitespaceWithNewlineOrNull() }
                ?.let {
                    if (node.textContains('\n')) {
                        // It can not be autocorrected as it might depend on the situation and code style what is preferred.
                        emit(
                            node.startOffset,
                            "A block comment after any other element on the same line must be separated by a new line",
                            false,
                        )
                    } else {
                        emit(
                            node.startOffset,
                            "A single line block comment after a code element on the same line must be replaced with an EOL comment",
                            true,
                        )
                        if (autoCorrect) {
                            node.upsertWhitespaceBeforeMe(" ")
                            node.replaceWithEndOfLineComment()
                        }
                    }
                }

            afterBlockComment
                .nextLeaf()
                .takeIf { !it.isWhitespaceWithNewlineOrNull() }
                ?.let { nextLeaf ->
                    emit(
                        nextLeaf.startOffset,
                        "A block comment may not be followed by any other element on that same line",
                        true
                    )
                    if (autoCorrect) {
                        nextLeaf.upsertWhitespaceBeforeMe(node.indent())
                    }
                }
        }
    }

    private fun ASTNode.replaceWithEndOfLineComment() {
        val content = text.removeSurrounding("/*", "*/").trim()
        val eolComment = PsiCommentImpl(EOL_COMMENT, "// $content")
        (this as LeafPsiElement).rawInsertBeforeMe(eolComment)
        rawRemove()
    }

    private fun ASTNode?.isWhitespaceWithNewlineOrNull() =
        this == null || this.isWhiteSpaceWithNewline()

    // TODO: Remove when ktlint suppression directive in comments are no longer supported
    private fun ASTNode?.isKtlintSuppressionDirective() =
        this
            ?.text
            ?.removePrefix("/*")
            ?.removeSuffix("*/")
            ?.trim()
            ?.let { it.startsWith("ktlint-enable") || it.startsWith("ktlint-disable") }
            ?: false
}

public val COMMENT_WRAPPING_RULE_ID: RuleId = CommentWrappingRule().ruleId
