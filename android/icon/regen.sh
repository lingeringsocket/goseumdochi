#!/bin/bash
# Regenerate icon resources from watchdog.png using ImageMagick

convert -resize 36x36 watchdog.png ../src/main/res/drawable-ldpi/icon.png
convert -resize 48x48 watchdog.png ../src/main/res/drawable-mdpi/icon.png
convert -resize 72x72 watchdog.png ../src/main/res/drawable-hdpi/icon.png
convert -resize 96x96 watchdog.png ../src/main/res/drawable-xhdpi/icon.png
