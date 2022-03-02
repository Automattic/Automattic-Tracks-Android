package com.automattic.android.tracks.experiment.example

import com.automattic.android.tracks.experiment.ExPlat
import com.automattic.android.tracks.experiment.Experiment

@Suppress("unused")
class ExampleExperiment(exPlat: ExPlat) : Experiment(
        name = "example_experiment",
        exPlat = exPlat
)
