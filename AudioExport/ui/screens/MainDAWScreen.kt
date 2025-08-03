@Composable
fun MainDAWScreen(
    viewModel: DAWViewModel = hiltViewModel()
) {
    val playbackState by viewModel.playbackState.observeAsState()
    val tracks by viewModel.tracks.observeAsState(emptyList())
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        // Transport Controls
        TransportControls(
            playbackState = playbackState ?: PlaybackState.STOPPED,
            onPlay = viewModel::play,
            onStop = viewModel::stop,
