#!/bin/bash
# Regenerate icon resources from leash.svg using ImageMagick

rsvg-convert -w 512 -h 512 -o leash.png leash.svg
rsvg-convert -w 36 -h 36 -o ../src/main/res/drawable-ldpi/icon.png leash.svg
rsvg-convert -w 48 -h 48 -o ../src/main/res/drawable-mdpi/icon.png leash.svg
rsvg-convert -w 72 -h 72 -o ../src/main/res/drawable-hdpi/icon.png leash.svg
rsvg-convert -w 96 -h 96 -o ../src/main/res/drawable-xhdpi/icon.png leash.svg
