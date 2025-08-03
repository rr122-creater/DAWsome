// ui/screens/DAWScreen.kt
@Composable
fun DAWScreen(
    viewModel: DAWViewModel = hiltViewModel()
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val currentProject by viewModel.currentProject.collectAsState()
    val tracks by viewModel.tracks.collectAsState()
    val playbackPosition by viewModel.playbackPosition.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadLastProject()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        // Top Bar
        DAWTopBar(
            projectName = currentProject?.name ?: "No Project",
            onMenuClick = { /* Open menu */ },
            onSaveClick = { viewModel.saveProject() }
        )
        
        // Transport Controls
        TransportControls(
            playbackState = playbackState,
            onPlay = viewModel::play,
            onStop = viewModel::stop,
            onRecord = viewModel::record,
            onRewind = viewModel::rewind,
            onFastForward = viewModel::fastForward
        )
        
        // Timeline
        TimelineView(
            playbackPosition = playbackPosition,
            totalDuration = currentProject?.totalDuration ?: 0L,
            onSeek = viewModel::seekTo
        )
        
        // Track List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(tracks) { track ->
                TrackItem(
                    track = track,
                    onVolumeChange = { volume -> viewModel.setTrackVolume(track.id, volume) },
                    onPanChange = { pan -> viewModel.setTrackPan(track.id, pan) },
                    onMuteToggle = { viewModel.toggleTrackMute(track.id) },
                    onSoloToggle = { viewModel.toggleTrackSolo(track.id) },
                    onRecordToggle = { viewModel.toggleTrackRecord(track.id) }
                )
            }
        }
        
        // Bottom Controls
        BottomControls(
            onAddTrack = viewModel::addTrack,
            onOpenMixer = { /* Open mixer */ },
            onOpenEffects = { /* Open effects */ }
        )
    }
}

@Composable
fun TransportControls(
    playbackState: PlaybackState,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onRecord: () -> Unit,
    onRewind: () -> Unit,
    onFastForward: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
