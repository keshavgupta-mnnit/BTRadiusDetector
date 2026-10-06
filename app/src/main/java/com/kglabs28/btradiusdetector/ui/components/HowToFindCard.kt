package com.kglabs28.btradiusdetector.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.ui.theme.SonarGreen
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/**
 * How-to-find card. Stateless — steps + callbacks hoisted via [Strings] / params.
 */
@Composable
fun HowToFindCard(
    steps: List<String>,
    onGotIt: () -> Unit,
    modifier: Modifier = Modifier,
    showClose: Boolean = false,
    onClose: (() -> Unit)? = null,
    title: String = Strings.howToFindTitle,
    gotItLabel: String = Strings.gotIt,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cornerRadiusDialog.scaled()),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationDialog.scaled())
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (showClose && onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.TopEnd).padding(Dimens.spacingXs.scaled())
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = Strings.closeDesc,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(
                modifier = Modifier.padding(
                    horizontal = Dimens.dialogPaddingH.scaled(),
                    vertical = Dimens.dialogPaddingV.scaled()
                ).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(Dimens.howToIllustrationSize.scaled()),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val maxR = size.width / 2 - Dimens.spacingSm.scaled().toPx()
                        for (i in 1..3) {
                            drawCircle(
                                color = SonarGreen.copy(alpha = 0.35f),
                                radius = maxR * (i / 3f),
                                style = Stroke(width = Dimens.borderWidthThin.scaled().toPx())
                            )
                        }
                        drawArc(
                            color = SonarGreen.copy(alpha = 0.7f),
                            startAngle = 200f,
                            sweepAngle = 140f,
                            useCenter = false,
                            style = Stroke(width = Dimens.borderWidthThin.scaled().toPx() * 2)
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = Dimens.alphaDotCenter),
                        modifier = Modifier.size(Dimens.iconIllustration.scaled())
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.spacingMd.scaled()))

                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = Dimens.textTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Dimens.spacingMd.scaled()))

                steps.forEachIndexed { index, step ->
                    HowToStepRow(number = index + 1, text = step)
                }

                if (trailingContent != null) {
                    Spacer(modifier = Modifier.height(Dimens.spacingLg.scaled()))
                    trailingContent()
                } else {
                    Spacer(modifier = Modifier.height(Dimens.spacingLg.scaled()))
                    PrimaryButton(label = gotItLabel, onClick = onGotIt, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun HowToStepRow(number: Int, text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(vertical = Dimens.spacingXs.scaled())
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.stepDotSize.scaled())
                .clip(CircleShape)
                .background(SonarGreen.copy(alpha = Dimens.alphaStepBg)),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", color = SonarGreen, fontWeight = FontWeight.Bold, fontSize = Dimens.textStepNumber)
        }
        Spacer(modifier = Modifier.width(Dimens.spacingMd.scaled()))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = Dimens.textLabel,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun HowToFindDialog(onDismiss: () -> Unit, steps: List<String> = Strings.howToSteps) {
    Dialog(onDismissRequest = onDismiss) {
        HowToFindCard(steps = steps, onGotIt = onDismiss, showClose = true, onClose = onDismiss)
    }
}

@Preview(showBackground = true)
@Composable
private fun HowToFindPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        HowToFindCard(steps = Strings.howToSteps, onGotIt = {}, showClose = true, onClose = {})
    }
}
