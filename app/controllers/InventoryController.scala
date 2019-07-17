package controllers

import play.api.mvc._
import play.api.libs.json._
import javax.inject.Inject
import db.InventoryDao
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.parsing.json.JSONObject


class InventoryController @Inject()(dao: InventoryDao) extends InjectedController {

  def getProductInventory(productId: String) = Action { request =>

    val dbResponse = Await.result(dao.fetchInventory(productId), 5000 millis)

    dbResponse.length match {
      case 1 => {
        val response: JsValue = JsObject(Seq("productId" -> JsNumber(dbResponse(0).id), "inventory" -> JsNumber(dbResponse(0).qty)))
        Ok(response)
      }
      case _ => {
        val response: JsValue = JsObject(Seq("error" -> JsString("unable to find productId")))
        Ok(response)
      }
    }

  }

  def updateProductInventory = Action(parse.json) { request =>

    val orderedInventory = request.body.validate[Map[String,Int]].getOrElse(Map[String, Int]())

    val orderedInventoryStatus = orderedInventory.map{ case (productId, orderQty) => {

      val dbResponse = Await.result(dao.fetchInventory(productId), 5000 millis)

      val isInStock = dbResponse.length match {
        case 1 => dbResponse(0).qty match {
          case inv if inv - orderQty > 0 => true
          case _ => false
          }
        case _ => false
      }

      new Tuple3(productId, orderQty, isInStock)
    }}

    val processTransaction = orderedInventoryStatus.foldLeft(true)( (acc, invTuple) => {
      acc match {
        case false => false
        case true => invTuple._3
      }
    })

    println("process transaction is: " + processTransaction)

//    now do processTransaction match: true => update db and send response, false => don't update db and send response
//    processTransaction match {
//      case true => {
//        orderedInventory.foreach( ***updateDB***)
//      }
//      case _ =>
//    }

    val responseMap = orderedInventoryStatus.foldLeft(Map.empty[String, Boolean]) ( (acc, invTuple) => {
      acc + (invTuple._1 -> invTuple._3)
    })

    val response = Json.toJson(responseMap)

    Ok(response)

    //println(orderedInventoryStatus)
    //val orderedInventoryMap = orderedInventoryStatus.map{ case(key, v1, v2) => {
    //Map(key -> v2)
    //}}
  }

}