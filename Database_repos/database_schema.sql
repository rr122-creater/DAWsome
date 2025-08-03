-- =============================================
-- Mobile DAW Database Schema
-- Version: 1.0
-- =============================================

-- Enable foreign key constraints
PRAGMA foreign_keys = ON;

-- =============================================
-- PROJECTS TABLE
-- =============================================
CREATE TABLE projects (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    modified_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    sample_rate INTEGER NOT NULL DEFAULT 44100,
    bit_depth INTEGER NOT NULL DEFAULT 16,
    tempo REAL NOT NULL DEFAULT 120.0,
    time_signature TEXT NOT NULL DEFAULT '4/4',
    project_path TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT 0,
    total_duration INTEGER DEFAULT 0, -- in milliseconds
    track_count INTEGER DEFAULT 0,
    version TEXT DEFAULT '1.0',
    
    -- Metadata
    artist TEXT,
    genre TEXT,
    key_signature TEXT,
    
    -- Project settings
    master_volume REAL DEFAULT 1.0,
    metronome_enabled BOOLEAN DEFAULT 0,
    count_in_bars INTEGER DEFAULT 0,
    
    CONSTRAINT chk_sample_rate CHECK (sample_rate IN (22050, 44100, 48000, 96000)),
    CONSTRAINT chk_bit_depth CHECK (bit_depth IN (16, 24, 32)),
    CONSTRAINT chk_tempo CHECK (tempo BETWEEN 60.0 AND 200.0),
    CONSTRAINT chk_master_volume CHECK (master_volume BETWEEN 0.0 AND 2.0)
);

-- =============================================
-- TRACKS TABLE
-- =============================================
CREATE TABLE tracks (
    id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL,
    name TEXT NOT NULL,
    track_index INTEGER NOT NULL,
    track_type TEXT NOT NULL DEFAULT 'AUDIO',
    
    -- Audio properties
    is_muted BOOLEAN NOT NULL DEFAULT 0,
    is_solo BOOLEAN NOT NULL DEFAULT 0,
    is_record_enabled BOOLEAN NOT NULL DEFAULT 0,
    is_armed BOOLEAN NOT NULL DEFAULT 0,
    
    -- Mix properties
    volume REAL NOT NULL DEFAULT 1.0,
    pan REAL NOT NULL DEFAULT 0.0,
    
    -- Visual properties
    color INTEGER NOT NULL DEFAULT 0xFF4CAF50,
    height INTEGER DEFAULT 80, -- track height in pixels
    
    -- Input/Output
    input_source TEXT DEFAULT 'MIC',
    output_bus TEXT DEFAULT 'MASTER',
    
    -- Monitoring
    monitor_mode TEXT DEFAULT 'OFF', -- OFF, INPUT, TAPE
    
    -- Timestamps
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    modified_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT chk_track_type CHECK (track_type IN ('AUDIO', 'MIDI', 'INSTRUMENT', 'BUS', 'AUX')),
    CONSTRAINT chk_volume CHECK (volume BETWEEN 0.0 AND 2.0),
    CONSTRAINT chk_pan CHECK (pan BETWEEN -1.0 AND 1.0),
    CONSTRAINT chk_monitor_mode CHECK (monitor_mode IN ('OFF', 'INPUT', 'TAPE')),
    CONSTRAINT unique_track_index UNIQUE (project_id, track_index)
);

-- =============================================
-- AUDIO CLIPS TABLE
-- =============================================
CREATE TABLE audio_clips (
    id TEXT PRIMARY KEY,
    track_id TEXT NOT NULL,
    name TEXT NOT NULL,
    file_path TEXT NOT NULL,
    
    -- Timeline position (in samples)
    start_time INTEGER NOT NULL DEFAULT 0,
    duration INTEGER NOT NULL,
    offset INTEGER NOT NULL DEFAULT 0, -- clip start offset
    
    -- Audio properties
    gain REAL NOT NULL DEFAULT 1.0,
    fade_in INTEGER NOT NULL DEFAULT 0, -- in samples
    fade_out INTEGER NOT NULL DEFAULT 0, -- in samples
    
    -- Playback properties
    is_looped BOOLEAN NOT NULL DEFAULT 0,
    loop_start INTEGER DEFAULT 0,
    loop_end INTEGER DEFAULT 0,
    
    -- Processing
    is_reversed BOOLEAN NOT NULL DEFAULT 0,
    pitch_shift REAL DEFAULT 0.0, -- in semitones
    time_stretch REAL DEFAULT 1.0, -- playback speed multiplier
    
    -- Metadata
    original_filename TEXT,
    file_size INTEGER,
    sample_rate INTEGER,
    channels INTEGER,
    bit_depth INTEGER,
    
    -- Timestamps
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    modified_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    
    FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE,
    CONSTRAINT chk_gain CHECK (gain BETWEEN 0.0 AND 4.0),
    CONSTRAINT chk_pitch_shift CHECK (pitch_shift BETWEEN -24.0 AND 24.0),
    CONSTRAINT chk_time_stretch CHECK (time_stretch BETWEEN 0.25 AND 4.0)
);

-- =============================================
-- EFFECTS TABLE
-- =============================================
CREATE TABLE effects (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    type TEXT NOT NULL,
    category TEXT NOT NULL,
    description TEXT,
    
    -- Effect metadata
    manufacturer TEXT DEFAULT 'MobileDAW',
    version TEXT DEFAULT '1.0',
    is_builtin BOOLEAN NOT NULL DEFAULT 1,
    
    -- Parameters schema (JSON)
    parameters_schema TEXT, -- JSON defining parameter structure
    default_preset TEXT,    -- JSON with default parameter values
    
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    
    CONSTRAINT chk_effect_type CHECK (type IN ('EQ', 'DYNAMICS', 'REVERB', 'DELAY', 'DISTORTION', 'FILTER', 'MODULATION', 'UTILITY')),
    CONSTRAINT chk_category CHECK (category IN ('BUILTIN', 'VST', 'AU', 'CUSTOM'))
);

-- =============================================
-- TRACK EFFECTS TABLE (Many-to-Many)
-- =============================================
CREATE TABLE track_effects (
    id TEXT PRIMARY KEY,
    track_id TEXT NOT NULL,
    effect_id TEXT NOT NULL,
    effect_order INTEGER NOT NULL DEFAULT 0,
    
    -- Effect state
    is_enabled BOOLEAN NOT NULL DEFAULT 1,
    is_bypassed BOOLEAN NOT NULL DEFAULT 0,
    
    -- Parameters (JSON)
    parameters TEXT, -- JSON object with current parameter values
    
    -- Presets
    current_preset TEXT,
    
    -- Timestamps
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    modified_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    
    FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE,
    FOREIGN KEY (effect_id) REFERENCES effects(id) ON DELETE CASCADE,
    CONSTRAINT unique_track_effect_order UNIQUE (track_id, effect_order)
);

-- =============================================
-- AUTOMATION TABLE
-- =============================================
CREATE TABLE automation (
    id TEXT PRIMARY KEY,
    track_id TEXT,
    effect_id TEXT,
    parameter_name TEXT NOT NULL,
    
    -- Automation data
    time_position INTEGER NOT NULL, -- in samples
    value REAL NOT NULL,
    curve_type TEXT DEFAULT 'LINEAR', -- LINEAR, BEZIER, STEP, EXPONENTIAL
    
    -- Curve control points (for bezier curves)
    control_point_1_x REAL,
    control_point_1_y REAL,
    control_point_2_x REAL,
    control_point_2_y REAL,
    
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    
    FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE,
    FOREIGN KEY (effect_id) REFERENCES effects(id) ON DELETE CASCADE,
    CONSTRAINT chk_curve_type CHECK (curve_type IN ('LINEAR', 'BEZIER', 'STEP', 'EXPONENTIAL'))
);

-- =============================================
-- PRESETS TABLE
-- =============================================
CREATE TABLE presets (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    effect_id TEXT NOT NULL,
    parameters TEXT NOT NULL, -- JSON with parameter values
    description TEXT,
    tags TEXT, -- comma-separated tags
    is_factory BOOLEAN NOT NULL DEFAULT 0,
    is_user BOOLEAN NOT NULL DEFAULT 1,
    
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    modified_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    
    FOREIGN KEY (effect_id) REFERENCES effects(id) ON DELETE CASCADE
);

-- =============================================
-- MARKERS TABLE
-- =============================================
CREATE TABLE markers (
    id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL,
    name TEXT NOT NULL,
    time_position INTEGER NOT NULL, -- in samples
    marker_type TEXT NOT NULL DEFAULT 'MARKER',
    color INTEGER DEFAULT 0xFFFFFF00,
    description TEXT,
    
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    
    FOREIGN KEY (project_id)
