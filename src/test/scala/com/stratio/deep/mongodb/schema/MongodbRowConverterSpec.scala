package com.stratio.deep.mongodb.schema

import com.mongodb.util.JSON
import com.mongodb.{DBObject, BasicDBObject}
import com.stratio.deep.mongodb.rdd.MongodbRDD
import org.apache.spark.sql.catalyst.types._
import org.apache.spark.sql.catalyst.expressions.{GenericRow, Row}
import org.apache.spark.sql.catalyst.types.StructType
import org.apache.spark.sql.test.TestSQLContext
import org.scalatest.{Matchers, FlatSpec }
import com.stratio.deep.DeepConfig
import com.stratio.deep.mongodb.{MongodbConfigBuilder, MongodbConfig}
import MongodbRowConverter._

import scala.collection.mutable.ArrayBuffer

/**
 * Created by jsantos on 6/02/15.
 */
class MongodbRowConverterSpec extends FlatSpec
with Matchers
with MongoEmbedDatabase
with TestBsonData {

  private val host: String = "localhost"
  private val port: Int = 12345
  private val database: String = "testDb"
  private val collection: String = "testCol"

  val testConfig = MongodbConfigBuilder()
    .set(MongodbConfig.Host,List(host + ":" + port))
    .set(MongodbConfig.Database,database)
    .set(MongodbConfig.Collection,collection)
    .set(MongodbConfig.SamplingRatio,1.0)
    .build()

  //  Sample values

  val valueWithType: List[(Any, StructField)] = List(
    1 -> new StructField(
      "att1",IntegerType,false),
    2.0 -> new StructField(
      "att2",DoubleType,false),
    "hi" -> new StructField(
      "att3",IntegerType,false),
    null.asInstanceOf[Any] -> new StructField(
      "att4",StringType,true),
    new ArrayBuffer[Int]().+=(1).+=(2).+=(3) -> new StructField(
      "att5",new ArrayType(IntegerType,false),false),
    new GenericRow(List(1,null).toArray) -> new StructField(
      "att6",new StructType(List(
        new StructField("att61",IntegerType ,false),
        new StructField("att62",IntegerType,true)
      )),false))

  val rowSchema = new StructType(valueWithType.map(_._2))

  val row = new GenericRow(valueWithType.map(_._1).toArray)

  val dbObject = JSON.parse(
    """{ "att5" : [ 1 , 2 , 3] ,
          "att4" :  null  ,
          "att3" : "hi" ,
          "att6" : { "att61" : 1 , "att62" :  null } ,
          "att2" : 2.0 ,
          "att1" : 1}""").asInstanceOf[DBObject]

  behavior of "The MongodbRowConverter"

  it should "be able to convert any value from a row into a dbobject field" in{
    toDBObject(row, rowSchema) should equal(dbObject)
  }

  it should "be able to convert any value from a dbobject field  into a row field" in{
    toSQL(dbObject,rowSchema) should equal(row)
  }

  it should "apply dbobject to row mapping in a RDD context" in {
    withEmbedMongoFixture(complexFieldAndType2) { mongodProc =>
      val mongodbRDD = new MongodbRDD(TestSQLContext, testConfig)
      val schema = MongodbSchema(mongodbRDD, 1.0).schema()
      val collected = toSQL(complexFieldAndType2.head,schema)
      MongodbRowConverter
        .asRow(schema,mongodbRDD)
        .collect().toList should equal(List(collected))
    }
  }

}