
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GNSSInfo(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar with back button
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Satellite Info",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info cards
        val infoList = listOf(
            "SVID" to "Satellite ID number.\nThis is a unique identifier for each satellite. It helps your device distinguish which satellite is sending signals.",
            "Constellation" to "Indicates which satellite system the satellite belongs to. GPS is American, GLONASS is Russian, Galileo is European, and BeiDou is Chinese.",
            "CN0" to "Carrier-to-noise ratio (signal quality) in dB-Hz.\nHigher values mean a clearer signal and better positioning accuracy.",
            "Elevation" to "Angle above horizon in degrees.\n90° is directly overhead, 0° is at the horizon.",
            "Azimuth" to "Compass direction from north in degrees.\nShows the horizontal direction to the satellite, measured clockwise from true north.",
            "Used in fix?" to "Whether this satellite is actively used for your location calculation.\nIf yes, it's contributing to your device's current position; if no, it's just tracked.",
            "Doppler" to "Doppler shift in m/s (satellite relative motion).\nIndicates the relative speed of the satellite toward or away from your device.",
            "Pseudorange uncertainty" to "Estimated error in pseudorange measurement (m/s).\nLower values indicate more precise distance measurements and better positioning accuracy.",
            "Distance" to "Estimated distance from the device to the satellite, error margin of about ~1-5km, depending on N2YO ephemeris data."
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(infoList) { (title, description) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
