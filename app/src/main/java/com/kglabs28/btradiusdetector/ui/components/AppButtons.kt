package com.kglabs28.btradiusdetector.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.font.FontWeight
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.ui.theme.SonarGreen
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/**
 * Primary filled pill button. Stateless; label + callback hoisted.
 */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(Dimens.gotItButtonH.scaled()),
        shape = RoundedCornerShape(Dimens.cornerRadiusPill.scaled()),
        colors = ButtonDefaults.buttonColors(
            containerColor = SonarGreen,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(Dimens.iconSizeXs.scaled()))
            Spacer(modifier = Modifier.width(Dimens.spacingSm.scaled()))
        }
        Text(label, fontWeight = FontWeight.Bold, fontSize = Dimens.textButton)
    }
}

/**
 * Teal outline action button (e.g. Buzz my watch). Stateless.
 */
@Composable
fun OutlineActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(Dimens.buzzButtonH.scaled()),
        shape = RoundedCornerShape(Dimens.cornerRadiusCardSmall.scaled()),
        border = BorderStroke(Dimens.borderWidthThin.scaled(), SonarGreen.copy(alpha = Dimens.alphaBuzzBorder)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SonarGreen)
    ) {
        if (content != null) content()
        else {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(Dimens.iconSizeXs.scaled()))
                Spacer(modifier = Modifier.width(Dimens.spacingSm.scaled()))
            }
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = Dimens.textBuzz)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ButtonsPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        androidx.compose.foundation.layout.Column {
            PrimaryButton(label = Strings.gotIt, onClick = {})
            OutlineActionButton(label = Strings.buzzMyWatch, onClick = {})
        }
    }
}
