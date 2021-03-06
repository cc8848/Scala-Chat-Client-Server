package br.unb.cic.redes.Model.models

import java.io.PrintStream
import java.net.Socket

import akka.actor.{Actor, Props}
import br.unb.cic.redes.Model.{Main, Manager, Messages}

import scala.collection.mutable.ListBuffer
import scala.io.BufferedSource
import scala.util.control.Breaks._

/**
  * Created by @brenoxp on 22/10/16.
  */

case class User(var nickname: String, socket: Socket) {

  private val connectionActor = Main.system.actorOf(Props(new Connection(socket, this)))

  /* Groups */
  private val groups = ListBuffer[Group]()

  def addGroup(group: Group) = groups += group
  def removeGroup(group: Group) = groups -= group
  var currentGroup: Group = null
  var away = false

  /* Connection Methods */
  def receiveMessage(message: Message) = Manager.receiveMessage(message)
  def sendMessage(message: Message) = connectionActor ! message

  // def stopConnection = connectionActor ! Messages.stop_connection
}


class Connection(socket: Socket, user: User) extends Actor {

  private val bufferedSource = new BufferedSource(socket.getInputStream())
  private val out = new PrintStream(socket.getOutputStream())

  private val listenerConnectionThread = createConnectionThread
  listenerConnectionThread.start()

  Manager.welcomeHelp(Message("/welcome", user))

  private def createConnectionThread = {
    new Thread(new Runnable {
      def run() {
        breakable {
          while (true) {
            val in = bufferedSource.getLines()
            val res = in.next()

//            if(res == "/leave") {
//              Manager.removeUser(user)
//              stopConnectionThread()
//              break
//            }

            user.receiveMessage(Message(res, user))
          }
        }
      }
    })
  }

  private def stopConnectionThread():Unit = {
    socket.close()
    listenerConnectionThread.interrupt()
  }

  def receive = {
    case Message(message, a) => sendMessage(Message(message))
  }

  private def sendMessage(message: Message) = {
    out.println(Message.prepareForClient(message.message))
  }

}