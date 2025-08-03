// ui/components/TrackItem.kt
@Composable
fun TrackItem(
    track: Track,
    onVolumeChange: (Float) -> Unit,
    onPanChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onSoloToggle: () -> Unit,
    onRecordToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        backgroundColor = Color(track.color).copy(alpha = 0.1f),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Track Controls Section
            TrackControlsSection(
                track = track,
                onMuteToggle = onMuteToggle,
                onSoloToggle = onSoloToggle,
                onRecordToggle = onRecordToggle,
                onVolumeChange = onVolumeChange,
                onPanChange = onPanChange,
                modifier = Modifier.width(200.dp)
            )
            
            // Track Timeline Section
            TrackTimelineSection(
                track = track,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TrackControlsSection(
    track: Track,
    onMuteToggle: () -> Unit,
    onSoloToggle: () -> Unit,
    onRecordToggle: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onPanChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Track Name
        Text(
            text = track.name,
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        // Control Buttons Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Record Button
            ControlButton(
                isActive = track.isRecordEnabled,
                activeColor = Color.Red,
                onClick = onRecordToggle,
                size = 24.dp
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_record),
                    contentDescription = "Record",
                    tint = Color.White
                )
            }
            
            // Mute Button
            ControlButton(
                isActive = track.isMuted,
                activeColor = Color.Orange,
                onClick = onMuteToggle,
                size = 24.dp
            ) {
                Text("M", fontSize = 10.sp, color = Color.White)
            }
            
            // Solo Button
            ControlButton(
                isActive = track.isSolo,
                activeColor = Color.Yellow,
                onClick = onSoloTog

// ui/components/TrackItem.kt (continued)
@Composable
private fun ControlButton(
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    size: Dp,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(size),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isActive) activeColor else Color.Gray,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(2.dp)
    ) {
        content()
    }
}

// Volume and Pan Sliders
Column {
    Text("Vol", fontSize = 8.sp)
    Slider(
        value = track.volume,
        onValueChange = onVolumeChange,
        valueRange = 0f..2f,
        modifier = Modifier.width(80.dp)
    )
    
    Text("Pan", fontSize = 8.sp)
    Slider(
        value = track.pan,
        onValueChange = onPanChange,
        valueRange = -1f..1f,
        modifier = Modifier.width(80.dp)
    )
}
