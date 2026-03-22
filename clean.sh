#!/bin/bash
# 清理构建依赖脚本

echo "清理构建依赖..."

rm -rf .gradle
rm -rf gradle-8.4
rm -rf android-sdk
rm -rf app/build
rm -rf gradle
rm -f *.idsig

echo "清理完成！"
echo "保留的文件："
ls -la
