package com.example.edureader.data.parser

import java.io.File

interface EpubStorage {
    fun getExtractionDir(file: File): File
}
