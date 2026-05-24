package com.thecalcurate.android.ui

import android.util.Log
import com.blongho.country_data.World
import com.thecalcurate.android.R

class CurrencyButton {
    companion object{
        fun getResource(code: String): Int {
            Log.i("CurrencyButton", "code: $code")
            return when (code) {
                "AED" -> R.drawable.aed
                "AFN" -> R.drawable.afn
                "ALL" -> R.drawable.all
                "AMD" -> R.drawable.amd
                "ANG" -> R.drawable.ang
                "AOA" -> R.drawable.aoa
                "ARS" -> R.drawable.ars
                "AUD" -> R.drawable.aud
                "AWG" -> R.drawable.awg
                "AZN" -> R.drawable.azn
                "BBD" -> R.drawable.bbd
                "BDT" -> R.drawable.bdt
                "BGN" -> R.drawable.bgn
                "BHD" -> R.drawable.bhd
                "BIF" -> R.drawable.bif
                "BMD" -> R.drawable.bmd
                "BND" -> R.drawable.bnd
                "BOB" -> R.drawable.bob
                "BRL" -> R.drawable.brl
                "BSD" -> R.drawable.bsd
                "BTN" -> R.drawable.btn
                "BYN" -> R.drawable.byn
                "BZD" -> R.drawable.bzd
                "CAD" -> R.drawable.cad
                "CDF" -> R.drawable.cdf
                "CHF" -> R.drawable.chf
                "CLP" -> R.drawable.clp
                "CNY" -> R.drawable.chn
                "COP" -> R.drawable.cop
                "CRC" -> R.drawable.crc
                "CUP" -> R.drawable.cup
                "CVE" -> R.drawable.cve
                "CZK" -> R.drawable.czk
                "DJF" -> R.drawable.djf
                "DKK" -> R.drawable.dkk
                "DOP" -> R.drawable.dop
                "DZD" -> R.drawable.dzd
                "EGP" -> R.drawable.egp
                "ERN" -> R.drawable.ern
                "ETB" -> R.drawable.etb
                "EUR" -> R.drawable.euro
                "FJD" -> R.drawable.fjd
                "GBP" -> R.drawable.pound
                "GEL" -> R.drawable.gel
                "GHS" -> R.drawable.ghs
                "GIP" -> R.drawable.gip
                "GMD" -> R.drawable.gmd
                "GNF" -> R.drawable.gnf
                "GTQ" -> R.drawable.gtq
                "GYD" -> R.drawable.gyd
                "HKD" -> R.drawable.hkd
                "HNL" -> R.drawable.hnl
                "HRK" -> R.drawable.hrk
                "HTG" -> R.drawable.htg
                "HUF" -> R.drawable.huf
                "IDR" -> R.drawable.idr
                "ILS" -> R.drawable.ils
                "INR" -> R.drawable.inr
                "IQD" -> R.drawable.iqd
                "IRR" -> R.drawable.irr
                "ISK" -> R.drawable.isk
                "JMD" -> R.drawable.jmd
                "JOD" -> R.drawable.jod
                "JPY" -> R.drawable.jpy
                "KES" -> R.drawable.kes
                "KGS" -> R.drawable.kgs
                "KHR" -> R.drawable.khr
                "KMF" -> R.drawable.kmf
                "KRW" -> R.drawable.krw
                "KWD" -> R.drawable.kwd
                "KYD" -> R.drawable.kyd
                "KZT" -> R.drawable.kzt
                "LAK" -> R.drawable.lak
                "LBP" -> R.drawable.lbp
                "LKR" -> R.drawable.lkr
                "LRD" -> R.drawable.lrd
                "LYD" -> R.drawable.lyd
                "MAD" -> R.drawable.mad
                "MDL" -> R.drawable.mdl
                "MGA" -> R.drawable.mga
                "MKD" -> R.drawable.mkd
                "MMK" -> R.drawable.mmk
                "MNT" -> R.drawable.mnt
                "MRU" -> R.drawable.mru
                "MUR" -> R.drawable.mur
                "MVR" -> R.drawable.mvr
                "MWK" -> R.drawable.mwk
                "MXN" -> R.drawable.mxn
                "MYR" -> R.drawable.myr
                "MZN" -> R.drawable.mzn
                "NAD" -> R.drawable.nad
                "NGN" -> R.drawable.ngn
                "NIO" -> R.drawable.nio
                "NOK" -> R.drawable.nok
                "NPR" -> R.drawable.npr
                "NZD" -> R.drawable.nzd
                "OMR" -> R.drawable.omr
                "PAB" -> R.drawable.pab
                "PEN" -> R.drawable.pen
                "PGK" -> R.drawable.pgk
                "PHP" -> R.drawable.php
                "PKR" -> R.drawable.pkr
                "PLN" -> R.drawable.pln
                "PYG" -> R.drawable.pyg
                "QAR" -> R.drawable.qar
                "RON" -> R.drawable.ron
                "RSD" -> R.drawable.rsd
                "RUB" -> R.drawable.rub
                "RWF" -> R.drawable.rwf
                "SAR" -> R.drawable.sar
                "SBD" -> R.drawable.sbd
                "SCR" -> R.drawable.scr
                "SDG" -> R.drawable.sdg
                "SEK" -> R.drawable.sek
                "SGD" -> R.drawable.sgd
                "SLE" -> R.drawable.sle
                "SLL" -> R.drawable.sll
                "SOS" -> R.drawable.sos
                "SRD" -> R.drawable.srd
                "SSP" -> R.drawable.ssp
                "STN" -> R.drawable.stn
                "SYP" -> R.drawable.syp
                "SZL" -> R.drawable.szl
                "THB" -> R.drawable.thb
                "TJS" -> R.drawable.tjs
                "TMT" -> R.drawable.tmt
                "TND" -> R.drawable.tnd
                "TOP" -> R.drawable.top
                "TRY" -> R.drawable.tryn
                "TWD" -> R.drawable.twd
                "TZS" -> R.drawable.tzs
                "UAH" -> R.drawable.uah
                "UGX" -> R.drawable.ugx
                "USD" -> R.drawable.usd
                "UYU" -> R.drawable.uyu
                "UZS" -> R.drawable.uzs
                "VES" -> R.drawable.ves
                "VND" -> R.drawable.vnd
                "VUV" -> R.drawable.vuv
                "WST" -> R.drawable.wst
                "XAF" -> R.drawable.xaf
                "XCD" -> R.drawable.xcd
                "XOF" -> R.drawable.xof
                "YER" -> R.drawable.yer
                "ZAR" -> R.drawable.zar
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
