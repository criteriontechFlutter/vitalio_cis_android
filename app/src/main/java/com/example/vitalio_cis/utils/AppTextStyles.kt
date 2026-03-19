package com.critetiontech.ctvitalio.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.vitalio_cis.R
import com.example.vitalio_cis.ui.theme.ThemeViewModel

val latoFont =FontFamily(
    Font(R.font.lato_regular, weight = FontWeight.Normal),
    Font(R.font.lato_bold, weight = FontWeight.Bold),
    Font(R.font.lato_italic, weight = FontWeight.Normal, style = FontStyle.Italic)
) // Replace with your Lato font family

class AppTextStyles(private val themeViewModel: ThemeViewModel) {

    /** Collect dynamic theme colors once */
    @Composable
    private fun currentColors() = themeViewModel.colorScheme.collectAsState().value

    /** Very Large Bold Primary */
    @Composable
    fun style24BCB(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = color ?: currentColors().text_color_black
    )

    @Composable
    fun style24BCN(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 24.sp,
        fontWeight = FontWeight.Normal,
        color = color ?: currentColors().text_color_black
    )


    /** Very Large Bold Primary */
    @Composable
    fun style24GCB(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = color ?: currentColors().text_color_black
    )

    @Composable
    fun style24GCN(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 24.sp,
        fontWeight = FontWeight.Normal,
        color = color ?: currentColors().textfeild_placehoder_color_grey
    )

    @Composable
    fun style24WCB(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = color ?: currentColors().text_color_white
    )

    @Composable
    fun style24WCN(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 24.sp,
        fontWeight = FontWeight.Normal,
        color = color ?: currentColors().text_color_white
    )




    /** Very Large Bold Primary */
    @Composable
    fun style16BCB(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = color ?: currentColors().text_color_black
    )

    @Composable
    fun style16BCN(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = color ?: currentColors().text_color_black
    )


    /** Very Large Bold Primary */
    @Composable
    fun style16GCB(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = color ?: currentColors().text_color_black
    )

    @Composable
    fun style16GCN(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = color ?: currentColors().textfeild_placehoder_color_grey
    )


    @Composable
    fun style16WCB(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = color ?: currentColors().text_color_white
    )

    @Composable
    fun style16WCN(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = color ?: currentColors().text_color_white
    )



    /** Very Large Bold Primary */
    @Composable
    fun style18BCB(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = color ?: currentColors().text_color_black
    )

    @Composable
    fun style18BCN(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal,
        color = color ?: currentColors().text_color_black
    )


    /** Very Large Bold Primary */
    @Composable
    fun style18GCB(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = color ?: currentColors().text_color_black
    )

    @Composable
    fun style18GCN(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal,
        color = color ?: currentColors().textfeild_placehoder_color_grey
    )


    @Composable
    fun style18WCB(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = color ?: currentColors().text_color_white
    )

    @Composable
    fun style18WCN(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal,
        color = color ?: currentColors().text_color_white
    )


    /** Very Large Bold Primary */
    @Composable
    fun style14BCB(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = color ?: currentColors().text_color_black
    )

    @Composable
    fun style14BCN(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = color ?: currentColors().text_color_black
    )


    /** Very Large Bold Primary */
    @Composable
    fun style14GCB(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = color ?: currentColors().text_color_black
    )

    @Composable
    fun style14GCN(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = color ?: currentColors().textfeild_placehoder_color_grey
    )

    /** Very Large Bold Primary */
    @Composable
    fun style14WCB(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = color ?: currentColors().text_color_white
    )

    @Composable
    fun style14WCN(color: Color? = null) = TextStyle(
        fontFamily = latoFont,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = color ?: currentColors().text_color_white
    )


    // You can add other styles (WCN, BCB, GCB, etc.) following this pattern
}