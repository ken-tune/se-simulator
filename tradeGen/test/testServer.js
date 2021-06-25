const http = require('http');
const concat = require('concat-stream');
const util = require("../modules/utility")

const SERVER_PORT = util.getEnvironmentValue("SERVER_PORT",8080)

http.createServer((request, response) => {
  request.setEncoding('utf8');
  request.on('data', chunk => {
    console.log(chunk);
  });
  response.end();
}).listen(SERVER_PORT);

console.log('server running on port '+SERVER_PORT);
