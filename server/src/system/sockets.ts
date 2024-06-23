import http from 'http'
import { Server } from 'socket.io'

import { config } from './config'
import { redis } from './redis'
import Cache from 'src/type/Cache'
import { timeStamp } from 'console'

const REDIS_KEY = "CloudChef"

export async function loadSocketClient(server: http.Server) {

  const io = new Server(server)

  io.on('connection', socket => {
    console.info(`New connection: ${socket.id}`)

    socket.on('QRCODE', (data) => {
      console.log(`QRCode Scanned from ${socket.id}: ${data}`)
    })

    socket.on('INIT', async (lastUpdated) => {
      if (lastUpdated) {
        const cache = await redis.get(REDIS_KEY);
        if (cache) {
          const cacheData = JSON.parse(cache);
          const filteredData = cacheData.filter((c: Cache) => {
            return c.timestamp > Number(lastUpdated);
          })
          socket.emit("DATA", JSON.stringify(filteredData));
        }
      }
    })
  })

  const sendData = async () => {
    const message = 50 + Math.random() * 50;
    const data = {
      timestamp: Date.now(),
      value: message
    }
    const socketData = [data];
    io.sockets.emit('DATA', JSON.stringify(socketData));

    const currentCache = await redis.get(REDIS_KEY);
    if (currentCache) {
      const cacheData = JSON.parse(currentCache);
      await redis.set(REDIS_KEY, JSON.stringify([...cacheData, data]));
    } else {
      await redis.set(REDIS_KEY, JSON.stringify((socketData)));
    }
  }

  setInterval(() => {
    sendData();
  }, 2000)
}
