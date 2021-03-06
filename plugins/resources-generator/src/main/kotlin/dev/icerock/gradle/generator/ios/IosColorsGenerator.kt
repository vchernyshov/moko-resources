/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.gradle.generator.ios

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import dev.icerock.gradle.generator.ColorNode
import dev.icerock.gradle.generator.ColorsGenerator
import dev.icerock.gradle.generator.ObjectBodyExtendable
import dev.icerock.gradle.generator.ios.IosMRGenerator.Companion.ASSETS_DIR_NAME
import dev.icerock.gradle.utils.ArgbColor
import dev.icerock.gradle.utils.parseRgbaColor
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.json
import kotlinx.serialization.json.jsonArray
import org.gradle.api.file.FileTree
import java.io.File

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
class IosColorsGenerator(
    colorsFileTree: FileTree
) : ColorsGenerator(colorsFileTree), ObjectBodyExtendable by IosGeneratorHelper() {
    override fun getImports(): List<ClassName> {
        return listOf(
            ClassName("dev.icerock.moko.graphics", "Color")
        )
    }

    override fun getClassModifiers(): Array<KModifier> = arrayOf(KModifier.ACTUAL)
    override fun getPropertyModifiers(): Array<KModifier> = arrayOf(KModifier.ACTUAL)

    override fun generateResources(resourcesGenerationDir: File, colors: List<ColorNode>) {
        val assetsDirectory = File(resourcesGenerationDir, ASSETS_DIR_NAME)

        colors.forEach { colorNode ->
            val assetDir = File(assetsDirectory, "${colorNode.name}.colorset")
            assetDir.mkdir()
            val contentsFile = File(assetDir, "Contents.json")
            contentsFile.createNewFile()

            val colorContentObj = if (colorNode.isThemed()) {
                @Suppress("MagicNumber")
                val lightColor = parseRgbaColor(colorNode.lightColor!!.toLong(16))
                @Suppress("MagicNumber")
                val darkColor = parseRgbaColor(colorNode.darkColor!!.toLong(16))

                /*
                Sets any color as light. Check about colors in the docs:
                https://developer.apple.com/documentation/xcode/supporting_dark_mode_in_your_interface
                 */
                val anyColor = buildColorIdiomJsonObj(lightColor)
                val lightColorObj = buildAppearancesIdiomJsonBlock("light", lightColor)
                val darkColorObj = buildAppearancesIdiomJsonBlock("dark", darkColor)

                jsonArray {
                    +anyColor
                    +lightColorObj
                    +darkColorObj
                }
            } else {
                @Suppress("MagicNumber")
                val singleColor = parseRgbaColor(colorNode.singleColor!!.toLong(16))
                jsonArray { +buildColorIdiomJsonObj(singleColor) }
            }

            val resultObj = json {
                "colors" to colorContentObj
                "info" to json {
                    "author" to "xcode"
                    "version" to 1
                }
            }
            contentsFile.writeText(resultObj.toString())
        }
    }

    private fun buildColorJsonObj(argbColor: ArgbColor): JsonObject = json {
        "color-space" to "srgb"
        "components" to json {
            "alpha" to argbColor.a
            "red" to argbColor.r
            "green" to argbColor.g
            "blue" to argbColor.b
        }
    }

    private fun buildColorIdiomJsonObj(argbColor: ArgbColor): JsonObject = json {
        "color" to buildColorJsonObj(argbColor)
        "idiom" to "universal"
    }

    private fun buildAppearancesIdiomJsonBlock(valueTag: String, argbColor: ArgbColor): JsonObject {
        return json {
            "appearances" to jsonArray {
                +json {
                    "appearance" to "luminosity"
                    "value" to valueTag
                }
            }
            "color" to buildColorJsonObj(argbColor)
            "idiom" to "universal"
        }
    }
}
