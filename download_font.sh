#!/bin/bash
# دانلود فونت Vazirmatn فارسی
# اجرا: chmod +x download_font.sh && ./download_font.sh

FONT_DIR="app/src/main/res/font"
mkdir -p "$FONT_DIR"

echo "📥 دانلود فونت Vazirmatn..."

curl -L -o "$FONT_DIR/vazirmatn_regular.ttf" \
  "https://github.com/rastikerdar/vazirmatn/raw/master/fonts/ttf/Vazirmatn-Regular.ttf"

curl -L -o "$FONT_DIR/vazirmatn_medium.ttf" \
  "https://github.com/rastikerdar/vazirmatn/raw/master/fonts/ttf/Vazirmatn-Medium.ttf"

curl -L -o "$FONT_DIR/vazirmatn_bold.ttf" \
  "https://github.com/rastikerdar/vazirmatn/raw/master/fonts/ttf/Vazirmatn-Bold.ttf"

curl -L -o "$FONT_DIR/vazirmatn_extrabold.ttf" \
  "https://github.com/rastikerdar/vazirmatn/raw/master/fonts/ttf/Vazirmatn-ExtraBold.ttf"

echo "✅ فونت‌ها با موفقیت دانلود شدند در $FONT_DIR"
