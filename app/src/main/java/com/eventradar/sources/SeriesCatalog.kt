package com.eventradar.sources

/** A ready-made racing calendar (feeds published by toomuchracing.com). */
data class Series(val group: String, val name: String, val feedUrl: String, val website: String? = null)

/**
 * The calendars listed at https://toomuchracing.com/calendar/ (checked 2 Nov 2026 — all 56 feeds live).
 * Each feed has one entry per race weekend with the circuit address; no ticket links or images,
 * so thumbnails come from the series' own website.
 */
object SeriesCatalog {
    val all: List<Series> = listOf(
        Series("Single seaters", "Formula 1", "https://calendar.google.com/calendar/ical/fa9bjl6tu13dd10b066stoo5do%40group.calendar.google.com/public/basic.ics", "https://www.formula1.com"),
        Series("Single seaters", "Formula E", "https://calendar.google.com/calendar/ical/vno0ntshopq0nmob26db2pcen8%40group.calendar.google.com/public/basic.ics", "https://www.fiaformulae.com"),
        Series("Single seaters", "IndyCar", "https://calendar.google.com/calendar/ical/hlskhf7l8ce7btind39bb9kf1o%40group.calendar.google.com/public/basic.ics", "https://www.indycar.com"),
        Series("Single seaters", "Super Formula", "https://calendar.google.com/calendar/ical/n00q1ce78ahlgmjn9qvs8nc6ko%40group.calendar.google.com/public/basic.ics", null),
        Series("Single seaters", "Formula 2", "https://calendar.google.com/calendar/ical/rttoqh7u6m247f2ub6c05m4pe4%40group.calendar.google.com/public/basic.ics", "https://www.fiaformula2.com"),
        Series("Single seaters", "Formula 3", "https://calendar.google.com/calendar/ical/sorhedtr7q5qmea6f0hvf20864%40group.calendar.google.com/public/basic.ics", "https://www.fiaformula3.com"),
        Series("Single seaters", "Indy NXT", "https://calendar.google.com/calendar/ical/4lrug7rrj92k8f9n2svn2fm2qo%40group.calendar.google.com/public/basic.ics", null),
        Series("Motorbikes", "MotoGP", "https://calendar.google.com/calendar/ical/832vbii8pmrvma356b4vn3v42c%40group.calendar.google.com/public/basic.ics", "https://www.motogp.com"),
        Series("Motorbikes", "World Superbikes (WSBK)", "https://calendar.google.com/calendar/ical/0rts2iu5gd88eis52c084ltlhc%40group.calendar.google.com/public/basic.ics", "https://www.worldsbk.com"),
        Series("Motorbikes", "Endurance World Championship (EWC)", "https://calendar.google.com/calendar/ical/c5bqcbfj7r80mcssavu0fheo3k%40group.calendar.google.com/public/basic.ics", null),
        Series("Motorbikes", "British Superbikes (BSB)", "https://calendar.google.com/calendar/ical/fjltmrhrg7riva80r4g8ruvbco%40group.calendar.google.com/public/basic.ics", "https://www.britishsuperbike.com"),
        Series("Motorbikes", "MotoAmerica", "https://calendar.google.com/calendar/ical/9sufdcnm1njfs086ig16asp5pg%40group.calendar.google.com/public/basic.ics", "https://www.motoamerica.com"),
        Series("Motorbikes", "MXGP Motocross", "https://calendar.google.com/calendar/ical/coufmu1k3fncfish6dpakqla0g%40group.calendar.google.com/public/basic.ics", "https://www.mxgp.com"),
        Series("Motorbikes", "Speedway GP", "https://calendar.google.com/calendar/ical/sn5j1rf8rau0ldvtda86tpsp8o%40group.calendar.google.com/public/basic.ics", null),
        Series("Motorbikes", "American Flat Track", "https://calendar.google.com/calendar/ical/tc2udocm4tp6hbj69ihd955cgc%40group.calendar.google.com/public/basic.ics", null),
        Series("Motorbikes", "AMA Supercross", "https://calendar.google.com/calendar/ical/cup9l2h4mlvattpndqsj1l24rs%40group.calendar.google.com/public/basic.ics", "https://www.supercrosslive.com"),
        Series("Motorbikes", "AMA Pro Motocross", "https://calendar.google.com/calendar/ical/rv875mb882njfjl2d9usc152sk%40group.calendar.google.com/public/basic.ics", "https://www.promotocross.com"),
        Series("NASCAR", "NASCAR Cup Series", "https://calendar.google.com/calendar/ical/db8c47ne2bt9qbld2mhdabm0u8%40group.calendar.google.com/public/basic.ics", "https://www.nascar.com"),
        Series("NASCAR", "NASCAR O'Reilly Series", "https://calendar.google.com/calendar/ical/po54lfbfrvlkrrhirlame40a6c%40group.calendar.google.com/public/basic.ics", "https://www.nascar.com"),
        Series("NASCAR", "NASCAR Truck Series", "https://calendar.google.com/calendar/ical/lnpvdfud8lhom7opdsnnbtu268%40group.calendar.google.com/public/basic.ics", "https://www.nascar.com"),
        Series("Le Mans & endurance", "World Endurance Championship (WEC)", "https://calendar.google.com/calendar/ical/61jccgg4rshh1temqk0dj4lens%40group.calendar.google.com/public/basic.ics", "https://www.fiawec.com"),
        Series("Le Mans & endurance", "European Le Mans Series", "https://calendar.google.com/calendar/ical/ur7thj1o6ctignecm0uia024js%40group.calendar.google.com/public/basic.ics", "https://www.europeanlemansseries.com"),
        Series("Le Mans & endurance", "Asian Le Mans Series", "https://www.google.com/calendar/ical/lilnartmo4uglqdpatsve4pido%40group.calendar.google.com/public/basic.ics", "https://www.asianlemansseries.com"),
        Series("Le Mans & endurance", "Michelin Le Mans Cup", "https://www.google.com/calendar/ical/niktsnpdfhu2bi3888ld8v24hc%40group.calendar.google.com/public/basic.ics", "https://www.lemanscup.com"),
        Series("IMSA", "IMSA WeatherTech SportsCar", "https://calendar.google.com/calendar/ical/njulhksvo83qeoruc3nhend9js%40group.calendar.google.com/public/basic.ics", "https://www.imsa.com"),
        Series("IMSA", "IMSA Michelin Pilot Challenge", "https://calendar.google.com/calendar/ical/bg95jli3rdktuuobbksko3h5vg%40group.calendar.google.com/public/basic.ics", "https://www.imsa.com"),
        Series("IMSA", "IMSA VP SportsCar Challenge", "https://calendar.google.com/calendar/ical/qevooec0eo13lpc26j6kik5goo%40group.calendar.google.com/public/basic.ics", "https://www.imsa.com"),
        Series("GT racing", "Super GT", "https://calendar.google.com/calendar/ical/5ni9rjbofnkfvmpidmjpep9ek0%40group.calendar.google.com/public/basic.ics", "https://supergt.net"),
        Series("GT racing", "DTM", "https://calendar.google.com/calendar/ical/0urnjij5qqj3ijoht52fdsqk18%40group.calendar.google.com/public/basic.ics", "https://www.dtm.com"),
        Series("GT racing", "Intercontinental GT Challenge", "https://calendar.google.com/calendar/ical/kcelko7ictk6okcf4peougahlo%40group.calendar.google.com/public/basic.ics", "https://www.intercontinentalgtchallenge.com"),
        Series("GT racing", "GT World Challenge Europe", "https://calendar.google.com/calendar/ical/drne83rrmn7m9baje25qh2248s%40group.calendar.google.com/public/basic.ics", "https://www.gt-world-challenge-europe.com"),
        Series("GT racing", "GT World Challenge America", "https://www.google.com/calendar/ical/1g47v5qu33g114060qa1ula9d0%40group.calendar.google.com/public/basic.ics", "https://www.gt-world-challenge-america.com"),
        Series("GT racing", "GT World Challenge Asia", "https://calendar.google.com/calendar/ical/plm3evhsd30l34r2tj68fh9mss%40group.calendar.google.com/public/basic.ics", null),
        Series("GT racing", "GT World Challenge Australia", "https://calendar.google.com/calendar/ical/31e7b509e16383e2c02a557c478ba3fe7cac843154c97ca5fbc77d69a578c253%40group.calendar.google.com/public/basic.ics", null),
        Series("GT racing", "Nürburgring Langstrecken (NLS)", "https://calendar.google.com/calendar/ical/f7ubn1ltpc4p7amil7kefgj754%40group.calendar.google.com/public/basic.ics", null),
        Series("GT racing", "24H Series", "https://www.google.com/calendar/ical/6rddivl20t6526fknlbhmhf6ps%40group.calendar.google.com/public/basic.ics", "https://www.24hseries.com"),
        Series("GT racing", "International GT Open", "https://www.google.com/calendar/ical/kug92q3u7fqcg2t0di3e2cklio%40group.calendar.google.com/public/basic.ics", "https://www.gtopen.net"),
        Series("GT racing", "British GT", "https://www.google.com/calendar/ical/6bh6kok6g3v97ogr2d1s2g1srs%40group.calendar.google.com/public/basic.ics", "https://www.britishgt.com"),
        Series("GT racing", "ADAC GT Masters", "https://www.google.com/calendar/ical/bo1ablitg2ecigfcdouq209vj0@group.calendar.google.com/public/basic.ics", null),
        Series("GT racing", "GT2 & GT4 European Series", "https://calendar.google.com/calendar/ical/pdbbgsms5dmvdhh6i4rucnlplg%40group.calendar.google.com/public/basic.ics", null),
        Series("GT racing", "GT America & GT4 America", "https://calendar.google.com/calendar/ical/eui5lon2nvcv1mbj1oj5cfle0c%40group.calendar.google.com/public/basic.ics", null),
        Series("GT racing", "SRO Japan Cup & GT Cup", "https://calendar.google.com/calendar/ical/57dafb59d25ddd478ffb783bf80602e371e6f9cc6c3772f82aca3e55d91eacc5%40group.calendar.google.com/public/basic.ics", null),
        Series("Touring cars", "Supercars", "https://calendar.google.com/calendar/ical/sd36enn936vc7dv35ucgulbjng%40group.calendar.google.com/public/basic.ics", "https://www.supercars.com"),
        Series("Touring cars", "British Touring Cars (BTCC)", "https://calendar.google.com/calendar/ical/9onbjrejbcstdm524hi0ush2t8%40group.calendar.google.com/public/basic.ics", "https://www.btcc.net"),
        Series("Touring cars", "TCR World Tour", "https://calendar.google.com/calendar/ical/ocjoa0k17eq7p81epuv77t4m18%40group.calendar.google.com/public/basic.ics", null),
        Series("Touring cars", "TCR Europe", "https://calendar.google.com/calendar/ical/1a762jm0fd49eei9c3lgjad11o%40group.calendar.google.com/public/basic.ics", null),
        Series("Rally & rallycross", "World Rally Championship (WRC)", "https://calendar.google.com/calendar/ical/fei68gpe16c85ed3jjdtvrn8ns%40group.calendar.google.com/public/basic.ics", "https://www.wrc.com"),
        Series("Rally & rallycross", "European Rally Championship (ERC)", "https://calendar.google.com/calendar/ical/tqcc98soldjf5ofrgmjc70eulk%40group.calendar.google.com/public/basic.ics", "https://www.fiaerc.com"),
        Series("Rally & rallycross", "World Rallycross", "https://calendar.google.com/calendar/ical/l61t6l5vpoihhie0o6df4j5h20%40group.calendar.google.com/public/basic.ics", "https://www.fiaworldrallycross.com"),
        Series("Rally & rallycross", "European Rallycross", "https://calendar.google.com/calendar/ical/u8atungvnj6ppj410hg5nuf7l0%40group.calendar.google.com/public/basic.ics", null),
        Series("Rally & rallycross", "World Rally-Raid (W2RC)", "https://calendar.google.com/calendar/ical/b772288cc7a8498d8b158e88d00b8667960ebef65f28468a85736f8648400676%40group.calendar.google.com/public/basic.ics", null),
        Series("Rally & rallycross", "Extreme H", "https://calendar.google.com/calendar/ical/4th6rmpe52qpjvmfuq4p41vgvo%40group.calendar.google.com/public/basic.ics", null),
        Series("Drag racing", "NHRA Drag Racing", "https://calendar.google.com/calendar/ical/kfm4mika8tdt7snq6naif5rt0c%40group.calendar.google.com/public/basic.ics", "https://www.nhra.com"),
        Series("Drag racing", "FIA European Drag Racing", "https://calendar.google.com/calendar/ical/ijlsqng1a5m5pdt4i1hm6lq2dk%40group.calendar.google.com/public/basic.ics", null),
        Series("Other", "Isle of Man TT, Goodwood & specials", "https://www.google.com/calendar/ical/76fdr76289paph73v5tjt6mpis%40group.calendar.google.com/public/basic.ics", null),
        Series("Other", "Other big sport (Tour de France, Olympics…)", "https://www.google.com/calendar/ical/hfs7bf429h03bnmo5eggvig37k%40group.calendar.google.com/public/basic.ics", null),
    )

    val groups: List<String> = all.map { it.group }.distinct()

    fun byFeed(url: String?): Series? = url?.let { u -> all.firstOrNull { it.feedUrl.equals(u.trim(), ignoreCase = true) } }
}
