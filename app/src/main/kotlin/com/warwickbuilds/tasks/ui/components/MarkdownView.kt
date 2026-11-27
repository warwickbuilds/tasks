package com.warwickbuilds.tasks.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.warwickbuilds.tasks.core.Markdown
import com.warwickbuilds.tasks.core.MdBlock
import com.warwickbuilds.tasks.core.MdSpan
import com.warwickbuilds.tasks.ui.theme.MonoStyle

/**
 * Draws a task's Markdown description. Checklist items can be ticked with [onToggle]
 * (it gets the item's source line). [maxBlocks] trims it for previews.
 */
@Composable
fun MarkdownView(
    markdown: String,
    modifier: Modifier = Modifier,
    onToggle: ((line: Int) -> Unit)? = null,
    maxBlocks: Int = Int.MAX_VALUE,
    color: Color = MaterialTheme.colorScheme.onSurface,
    accent: Color = MaterialTheme.colorScheme.primary,
    compact: Boolean = false,
) {
    val blocks = remember(markdown) { Markdown.parse(markdown) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)) {
        blocks.take(maxBlocks).forEach { block -> Block(block, onToggle, color, accent, compact) }
    }
}

@Composable
private fun Block(block: MdBlock, onToggle: ((Int) -> Unit)?, color: Color, accent: Color, compact: Boolean) {
    val type = MaterialTheme.typography
    val body = if (compact) type.bodyMedium else type.bodyLarge
    when (block) {
        is MdBlock.Heading -> Text(
            spans(block.spans, accent),
            style = when (block.level) {
                1 -> type.titleLarge
                2 -> type.titleMedium
                else -> type.titleSmall.copy(fontWeight = FontWeight.Bold)
            },
            color = color,
            modifier = Modifier.padding(top = if (compact) 0.dp else 6.dp),
        )
        is MdBlock.Paragraph -> Text(spans(block.spans, accent), style = body, color = color)
        is MdBlock.Quote -> Row {
            Box(
                Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp)),
            )
            Text(
                spans(block.spans, accent),
                style = body.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        is MdBlock.Code -> Text(
            block.text,
            style = MonoStyle,
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                .padding(12.dp),
        )
        MdBlock.Rule -> HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        is MdBlock.Item -> ListItemRow(block, onToggle, color, accent, body)
    }
}

@Composable
private fun ListItemRow(item: MdBlock.Item, onToggle: ((Int) -> Unit)?, color: Color, accent: Color, style: TextStyle) {
    val indent = (item.depth * 20).dp
    val marker = item.marker
    val checked = marker is MdBlock.Marker.Check && marker.checked
    val rowModifier = if (marker is MdBlock.Marker.Check && onToggle != null && item.line >= 0) {
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox) { onToggle(item.line) }
            .semantics { contentDescription = (if (checked) "Ticked: " else "Not ticked: ") + item.spans.joinToString("") { it.text } }
    } else {
        Modifier.fillMaxWidth()
    }
    Row(rowModifier.padding(start = indent, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.width(26.dp).padding(top = 3.dp), contentAlignment = Alignment.CenterStart) {
            when (marker) {
                is MdBlock.Marker.Check -> CheckBoxMark(checked, accent)
                is MdBlock.Marker.Number -> Text("${marker.n}.", style = style, color = color.copy(alpha = 0.7f))
                MdBlock.Marker.Bullet -> Box(
                    Modifier
                        .padding(start = 5.dp, top = 6.dp)
                        .size(6.dp)
                        .background(color.copy(alpha = 0.6f), RoundedCornerShape(3.dp)),
                )
            }
        }
        Text(
            spans(item.spans, accent),
            style = if (checked) style.copy(textDecoration = TextDecoration.LineThrough) else style,
            color = if (checked) color.copy(alpha = 0.55f) else color,
        )
    }
}

@Composable
private fun CheckBoxMark(checked: Boolean, accent: Color) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        Modifier
            .size(18.dp)
            .background(if (checked) accent else Color.Transparent, shape)
            .then(if (checked) Modifier else Modifier.border(1.5.dp, accent, shape)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(14.dp))
    }
}

/** Turns styled spans into an AnnotatedString; links open in the browser. */
fun spans(list: List<MdSpan>, accent: Color): AnnotatedString = buildAnnotatedString {
    for (s in list) {
        val style = SpanStyle(
            fontWeight = if (s.bold) FontWeight.Bold else null,
            fontStyle = if (s.italic) FontStyle.Italic else null,
            textDecoration = when {
                s.strike -> TextDecoration.LineThrough
                else -> null
            },
            fontFamily = if (s.code) FontFamily.Monospace else null,
            background = if (s.code) accent.copy(alpha = 0.12f) else Color.Unspecified,
        )
        val url = s.link?.takeIf { it.startsWith("https://") || it.startsWith("http://") || it.startsWith("mailto:") }
        if (url != null) {
            withLink(LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = accent, textDecoration = TextDecoration.Underline)))) {
                withStyle(style) { append(s.text) }
            }
        } else {
            withStyle(style) { append(s.text) }
        }
    }
}
