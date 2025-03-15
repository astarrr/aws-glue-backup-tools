package model

import upickle.default.*


case class Database (
                      name: String,
                      description: String,
                      location: String,
                      parameters: Map[String, String]
                    )
  derives ReadWriter
