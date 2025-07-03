package com.test.a

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.*

data class Node(val id: Long, val lat: Double, val lon: Double)
data class Way(val id : Long, val nodeRefs: List<Long>)
data class GpsPoint(val lat: Double, val lon: Double, val angle: Double, val speed: Double, val hdop: Double)

fun parseOsmFile(filePath: String): Pair<Map<Long, com.test.a.model.Node>, List<com.test.a.model.Way>>{
    val nodes = mutableMapOf<Long, com.test.a.model.Node>()
    val ways = mutableListOf<com.test.a.model.Way>()

    val doc = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(File(filePath))

    val nodeElements = doc.getElementsByTagName("node")
    for (i in 0 until nodeElements.length){
        val e = nodeElements.item(i) as Element
        val id = e.getAttribute("id").toLong()
        val lat = e.getAttribute("lat").toDouble()
        val lon = e.getAttribute("lon").toDouble()
        nodes[id] = com.test.a.model.Node(id, lat, lon)
    }

    val wayElements = doc.getElementsByTagName("way")
    for (i in 0 until wayElements.length){
        val e = wayElements.item(i) as Element
        val id = e.getAttribute("id").toLong()
        val ndList = e.getElementsByTagName("nd")
        val refs = mutableListOf<Long>()
        for (j in 0 until ndList.length){
            val nd = ndList.item(j) as Element
            refs.add(nd.getAttribute("ref").toLong())
        }
        ways.add(com.test.a.model.Way(id, refs))
    }

    return Pair(nodes, ways)
}

fun parseGpsCsv(filePath: String): List<com.test.a.model.GpsPoint>{
    return File(filePath).readLines()
        .drop(1)
        .mapNotNull{
            val parts = it.split(",")
            if (parts.size < 5) return@mapNotNull null
            com.test.a.model.GpsPoint(
                lat = parts[0].toDouble(),
                lon = parts[1].toDouble(),
                angle = parts[2].toDouble(),
                speed = parts[3].toDouble(),
                hdop = parts[4].toDouble()
            )
        }
}

fun projectToSegment(p: GpsPoint, a: Node, b: Node): Pair<Double, Pair<Double, Double>> {
    val dx = b.lat - a.lat
    val dy = b.lon - a.lon
    if (dx == 0.0 && dy == 0.0) return com.test.a.util.haversine(p.lat, p.lon, a.lat, a.lon) to (a.lat to a.lon)

    val t = ((p.lat - a.lat) * dx + (p.lon - a.lon) * dy) / (dx*dx + dy*dy)
    val clampedT = t.coerceIn(0.0, 1.0)
    val projLat = a.lat + clampedT * dx
    val projLon = a.lon + clampedT * dy
    val dist = com.test.a.util.haversine(p.lat, p.lon, projLat, projLon)

    return dist to (projLat to projLon)
}

fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double{
    val R = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val rLat1 = Math.toRadians(lat1)
    val rLat2 = Math.toRadians(lat2)
    val a = sin(dLat/2).pow(2) + cos(rLat1) * cos(rLat2) * sin(dLon/2).pow(2)
    return 2*R* atan2(sqrt(a), sqrt(1-a))
}

fun matchGpsToRoad(
    gpsPoints: List<GpsPoint>,
    nodes: Map<Long, Node>,
    ways: List<Way>,
    validWayIds: Set<Long>,
    thresholdMeters: Double = 30.0
): List<Triple<GpsPoint, Long?, Boolean>>{
    val waySegments = ways.flatMap{ way ->
        way.nodeRefs.zipWithNext().mapNotNull {(startId, endId) ->
            val start = nodes[startId]
            val end = nodes[endId]
            if (start != null && end != null) Triple(way.id, start, end) else null
        }
    }

    return gpsPoints.map{ gps ->
        var bestMatch: Triple<Long, Node, Node>? = null
        var minDistance = Double.MAX_VALUE

        for ((wayId, start, end) in waySegments){
            val (dist, _) = projectToSegment(gps, start, end)
            if (dist < minDistance){
                minDistance = dist
                bestMatch = Triple(wayId, start, end)
            }
        }

        val matchedWayId = bestMatch?.first
        val isOutOfPath = matchedWayId != null && matchedWayId !in validWayIds

        if (minDistance <= thresholdMeters) Triple(gps,matchedWayId, isOutOfPath)
        else Triple(gps,null,true)
    }
}

fun main(){
    val osmFilePath = "/MAP/roads.osm"
    val gpsCsvFilePath = "/GPS/gps_left_turn.csv"

    val (nodes, ways) = parseOsmFile(osmFilePath)
    val gpsPoints = parseGpsCsv(gpsCsvFilePath)

    val validWayIds = setOf(521766182L, 990628459L, 472042763L, 218864485L, 520307304L)

    val matched = matchGpsToRoad(gpsPoints, nodes, ways, validWayIds)

    matched.forEachIndexed{ idx, (gps, wayId, isOut) ->
        println("[$idx] GPS(${gps.lat}, ${gps.lon}) -> wayId=${wayId ?: "NONE"} | ${if(isOut) "경로 이탈" else "정상"}")
    }
}