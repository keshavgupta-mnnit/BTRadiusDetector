package com.kglabs28.btradiusdetector.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled
import com.kglabs28.btradiusdetector.utils.scaledDp

@Composable
fun PermissionRationaleDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.permissionsRequiredTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = { Text(Strings.permissionsRequiredBody, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { Button(onClick = onConfirm) { Text(Strings.continueLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.notNow) } },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.primary,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun PermissionRequestContent(onRequestPermissions: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Dimens.spacingXl.scaled()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(Dimens.iconSizeLarge.scaled()), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(Dimens.spacingLg.scaled()))
        Text(Strings.permissionsRequiredTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(Dimens.spacingSm.scaled()))
        Text(Strings.permissionsRequiredBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(Dimens.spacingXl.scaled()))
        Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Dimens.cornerRadiusButton.scaled())) {
            Text(Strings.grantPermissions)
        }
    }
}