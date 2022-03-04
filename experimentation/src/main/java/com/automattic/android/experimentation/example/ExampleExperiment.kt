package com.automattic.android.experimentation.example

import com.automattic.android.experimentation.ExPlat
import com.automattic.android.experimentation.Experiment

@Suppress("unused")
class ExampleExperiment(exPlat: ExPlat) : Experiment(
        name = "example_experiment",
        exPlat = exPlat
)
