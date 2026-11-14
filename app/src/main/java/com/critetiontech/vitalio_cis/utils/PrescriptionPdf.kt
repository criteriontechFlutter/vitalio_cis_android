package com.critetiontech.ctvitalio.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File

/**
 * Renders a prescription to an A4 PDF in the app's cache and shares it. Built with the platform
 * PdfDocument, so no extra library; text wraps and flows onto new pages.
 */
object PrescriptionPdf {

    data class Medicine(val name: String, val instruction: String)

    data class Content(
        val heading: String,
        val lines: List<String>,          // "Created by: Dr. X", "Date: …"
        val medicinesTitle: String,
        val medicines: List<Medicine>,
        val sections: List<Pair<String, String>>, // ("Diagnosis:", "…"), ("Notes:", "…")
        val footer: String,
    )

    private const val PAGE_W = 595   // A4 in PostScript points
    private const val PAGE_H = 842
    private const val MARGIN = 48f

    fun share(context: Context, content: Content, fileName: String, chooserTitle: String): Boolean = runCatching {
        val file = write(context, content, fileName)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess

    private fun write(context: Context, c: Content, fileName: String): File {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        // Only the latest share is needed; don't let old prescriptions pile up in the cache
        dir.listFiles()?.filter { it.name.endsWith(".pdf") }?.forEach { it.delete() }
        val file = File(dir, fileName)

        val doc = PdfDocument()
        val width = (PAGE_W - 2 * MARGIN).toInt()
        val accent = Color.rgb(23, 101, 242)
        val ink = Color.rgb(24, 32, 43)
        val muted = Color.rgb(91, 101, 116)

        fun paint(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        }

        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            y = MARGIN
        }

        fun text(s: String, p: TextPaint, indent: Float = 0f, after: Float = 4f) {
            if (s.isBlank()) return
            val layout = StaticLayout.Builder.obtain(s, 0, s.length, p, width - indent.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1f)
                .build()
            if (y + layout.height > PAGE_H - MARGIN) newPage()
            val canvas = page!!.canvas
            canvas.save()
            canvas.translate(MARGIN + indent, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + after
        }

        fun rule() {
            if (y + 12 > PAGE_H - MARGIN) newPage()
            page!!.canvas.drawLine(MARGIN, y + 4, PAGE_W - MARGIN, y + 4, Paint().apply { color = Color.rgb(226, 230, 236); strokeWidth = 1f })
            y += 14f
        }

        newPage()
        text(c.heading, paint(20f, accent, bold = true), after = 6f)
        c.lines.forEach { text(it, paint(11f, muted), after = 2f) }
        y += 8f
        rule()

        text(c.medicinesTitle, paint(13f, ink, bold = true), after = 8f)
        c.medicines.forEach { m ->
            text("•  ${m.name}", paint(12f, ink, bold = true), after = 1f)
            text(m.instruction, paint(11f, muted), indent = 14f, after = 8f)
        }

        c.sections.forEach { (title, body) ->
            y += 4f
            rule()
            text(title, paint(13f, ink, bold = true), after = 4f)
            text(body, paint(11f, muted), after = 6f)
        }

        y += 10f
        rule()
        text(c.footer, paint(9f, muted))

        page?.let { doc.finishPage(it) }
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
