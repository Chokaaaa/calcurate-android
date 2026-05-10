package com.thecalcurate.android.ui

import android.util.Log
import com.blongho.country_data.World
import com.thecalcurate.android.R

class CurrencyButton {
    companion object{
        fun getResource(code: String): Int {
            Log.i("CurrencyButton", "code: $code")
            return when (code) {
                "USD" -> R.drawable.usd
                "EUR" -> R.drawable.euro
                "GBP" -> R.drawable.pound
//                "AFN" -> R.drawable.afn
//                "ALL" -> R.drawable.all
//                "DZD" -> R.drawable.dzd
//                "AOA" -> R.drawable.aoa
                "ARS" -> R.drawable.ars
//                "AMD" -> R.drawable.amd
//                "AWG" -> R.drawable.awg
                "AUD" -> R.drawable.aud
//                "AZN" -> R.drawable.azn
//                "BSD" -> R.drawable.bsd
//                "BHD" -> R.drawable.bhd
//                "BDT" -> R.drawable.bdt
//                "BBD" -> R.drawable.bbd
                "BYN" -> R.drawable.byn
//                "BZD" -> R.drawable.bzd
//                "BMD" -> R.drawable.bmd
                "BTN" -> R.drawable.btn
//                "BOB" -> R.drawable.bob
//                "BRL" -> R.drawable.brl
//                "BND" -> R.drawable.bnd
//                "BGN" -> R.drawable.bgn
//                "BIF" -> R.drawable.bif
//                "XAF" -> R.drawable.xaf
//                "KHR" -> R.drawable.khr
//                "CAD" -> R.drawable.cad
//                "CVE" -> R.drawable.cve
//                "KYD" -> R.drawable.kyd
//                "CZK" -> R.drawable.czk
                "CLP" -> R.drawable.clp
//                "CNY" -> R.drawable.cny
//                "COP" -> R.drawable.cop
//                "KMF" -> R.drawable.kmf
//                "CDF" -> R.drawable.cdf
//                "CRC" -> R.drawable.crc
//                "HRK" -> R.drawable.hrk
//                "CUP" -> R.drawable.cup
//                "DKK" -> R.drawable.dkk
//                "DJF" -> R.drawable.djf
                "STN" -> R.drawable.stn
//                "DOP" -> R.drawable.dop
//                "XCD" -> R.drawable.xcd
//                "EGP" -> R.drawable.egp
                "ERN" -> R.drawable.ern
//                "ETB" -> R.drawable.etb
//                "FJD" -> R.drawable.fjd
//                "GMD" -> R.drawable.gmd
                "GEL" -> R.drawable.gel
//                "GHS" -> R.drawable.ghs
//                "GIP" -> R.drawable.gip
//                "GTQ" -> R.drawable.gtq
//                "GNF" -> R.drawable.gnf
//                "GYD" -> R.drawable.gyd
//                "HKD" -> R.drawable.hkd
//                "HTG" -> R.drawable.htg
//                "HNL" -> R.drawable.hnl
//                "HUF" -> R.drawable.huf
//                "ISK" -> R.drawable.isk
//                "INR" -> R.drawable.inr
//                "IDR" -> R.drawable.idr
//                "IRR" -> R.drawable.irr
//                "IQD" -> R.drawable.iqd
//                "ILS" -> R.drawable.ils
//                "JMD" -> R.drawable.jmd
//                "JOD" -> R.drawable.jod
//                "JPY" -> R.drawable.jpy
                "KZT" -> R.drawable.kzt
//                "KES" -> R.drawable.kes
//                "KWD" -> R.drawable.kwd
//                "KGS" -> R.drawable.kgs
//                "LAK" -> R.drawable.lak
//                "LBP" -> R.drawable.lbp
//                "LRD" -> R.drawable.lrd
//                "LYD" -> R.drawable.lyd
//                "MKD" -> R.drawable.mkd
//                "MGA" -> R.drawable.mga
//                "MWK" -> R.drawable.mwk
//                "MYR" -> R.drawable.myr
//                "MVR" -> R.drawable.mvr
//                "MUR" -> R.drawable.mur
                "MRU" -> R.drawable.mru
//                "MXN" -> R.drawable.mxn
//                "MDL" -> R.drawable.mdl
//                "MNT" -> R.drawable.mnt
//                "MAD" -> R.drawable.mad
//                "MZN" -> R.drawable.mzn
//                "MMK" -> R.drawable.mmk
                "NZD" -> R.drawable.nzd
//                "NAD" -> R.drawable.nad
//                "NPR" -> R.drawable.npr
                "ANG" -> R.drawable.ang
//                "NIO" -> R.drawable.nio
//                "NGN" -> R.drawable.ngn
//                "NOK" -> R.drawable.nok
//                "OMR" -> R.drawable.omr
//                "PKR" -> R.drawable.pkr
//                "PAB" -> R.drawable.pab
//                "PGK" -> R.drawable.pgk
//                "PYG" -> R.drawable.pyg
//                "PEN" -> R.drawable.pen
//                "PHP" -> R.drawable.php
//                "PLN" -> R.drawable.pln
//                "QAR" -> R.drawable.qar
//                "RON" -> R.drawable.ron
//                "RUB" -> R.drawable.rub
//                "RWF" -> R.drawable.rwf
//                "ZAR" -> R.drawable.zar
                "KRW" -> R.drawable.krw
//                "SSP" -> R.drawable.ssp
                "WST" -> R.drawable.wst
//                "SAR" -> R.drawable.sar
                "RSD" -> R.drawable.rsd
//                "SCR" -> R.drawable.scr
                "SLE" -> R.drawable.sle
                "CNY" -> R.drawable.chn
//                "SGD" -> R.drawable.sgd
//                "SBD" -> R.drawable.sbd
//                "SOS" -> R.drawable.sos
//                "LKR" -> R.drawable.lkr
//                "SDG" -> R.drawable.sdg
//                "SRD" -> R.drawable.srd
//                "SZL" -> R.drawable.szl
//                "CHF" -> R.drawable.chf
//                "SEK" -> R.drawable.sek
//                "SYP" -> R.drawable.syp
                "TWD" -> R.drawable.twd
//                "TJS" -> R.drawable.tjs
//                "TZS" -> R.drawable.tzs
//                "THB" -> R.drawable.thb
//                "TOP" -> R.drawable.top
//                "TND" -> R.drawable.tnd
//                "TRY" -> R.drawable.tryn
//                "TMT" -> R.drawable.tmt
                "AED" -> R.drawable.aed
//                "UGX" -> R.drawable.ugx
//                "UAH" -> R.drawable.uah
//                "UYU" -> R.drawable.uyu
                "UZS" -> R.drawable.uzs
//                "VUV" -> R.drawable.vuv
//                "VES" -> R.drawable.ves
//                "VND" -> R.drawable.vnd
//                "XOF" -> R.drawable.xof
//                "YER" -> R.drawable.yer
                "ZMW" -> R.drawable.zmw
                else -> {
                    val curList = World.getAllCurrencies().filter { it.code == code }
                    if (curList.isNotEmpty()) {
                        World.getFlagOf(curList[0]!!.country)
                    } else {
                        R.drawable.aed
                    }
                }
            }
        }
    }
}