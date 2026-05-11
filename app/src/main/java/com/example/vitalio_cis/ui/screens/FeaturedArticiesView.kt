package com.example.vitalio_cis.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme

@Composable
fun ArticleDetailScreen() {

    var ackExpanded by remember { mutableStateOf(true) }
    var fundingExpanded by remember { mutableStateOf(true) }

    val colors = LocalMyColorScheme.current
    CommonAppBar(
        title = "Carbohydrate antigen 125 (CA125) follo...",
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {

                Text(
                    "Published: 23 December, 2025",
                    style = AppTextStyles.style12GCN()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Carbohydrate antigen 125 (CA125) following acute myocardial infarction: effects of empagliflozin and association with heart failure readouts in the EMMY trial",

                    style = AppTextStyles.style14BCN()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Ahmed M. Hassan, Faisal Aziz, Norbert J. Tripolt, Markus Herrmann, Harald Sourij & Dirk von Lewinski",

                    style = AppTextStyles.style12GCN()
                )

                Spacer(modifier = Modifier.height(16.dp))

                /* ---------- REFERENCES BOX ---------- */

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.dashboardContainerColor, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {

                    Column {

                        Text(
                            "3. Feng, R., Zhang, Z. & Fan, C. Carbohydrate antigen 125 in congestive heart failure: ready for clinical application? Front. Oncol. 13, 1161723 (2023).",

                            style = AppTextStyles.style12GCN()
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "4. Núñez, J. et al. Improvement in risk stratification with the combination of the tumour marker antigen carbohydrate 125 and brain natriuretic peptide in patients with acute heart failure. Eur. Heart J. 31, 1752–1763 (2010).",

                            style = AppTextStyles.style12GCN()
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "5. Núñez, J. et al. CA125-Guided diuretic treatment versus usual care in patients with acute heart failure and renal dysfunction. Am. J. Med. 133, 370–380e4 (2020).",

                            style = AppTextStyles.style12GCN()
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "6. Miñana, G. et al. Factors associated with plasma antigen carbohydrate 125 and amino-terminal pro-B-type natriuretic peptide concentrations in acute heart failure. Eur. Heart J. Acute Cardiovasc. Care. 9, 437–447 (2020).",

                            style = AppTextStyles.style12GCN()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                /* ---------------- ACKNOWLEDGEMENT ---------------- */

                ExpandableSection(
                    title = "Acknowledgements",
                    expanded = ackExpanded,
                    onToggle = { ackExpanded = !ackExpanded },
                    content = "The CA125 immunoassays for this study were kindly provided by Abbott GmbH, Vienna, Austria."
                )

                Spacer(modifier = Modifier.height(12.dp))

                /* ---------------- FUNDING ---------------- */

                ExpandableSection(
                    title = "Funding",
                    expanded = fundingExpanded,
                    onToggle = { fundingExpanded = !fundingExpanded },
                    content = "The EMMY study was funded by an unrestricted grant from Boehringer Ingelheim (no. 1245.151). HS’s research is supported by the Austrian Science Fund (FWF grant KL-1076, PIN8074224) and the Horizon Europe project."
                )
            }
        }
    }
}

/* ---------------- EXPANDABLE ---------------- */

@Composable
fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: String
) {

    val colors = LocalMyColorScheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.dashboardContainerColor, RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .padding(12.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                title,

                style = AppTextStyles.style14BCN()
            )

            Icon(
                if (expanded) Icons.Default.KeyboardArrowUp
                else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }

        if (expanded) {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                content,
                style = AppTextStyles.style12GCN()
            )
        }
    }
}