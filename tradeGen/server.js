const http = require('http');
const concat = require('concat-stream');

http.createServer((request, response) => {
  request.setEncoding('utf8');
  request.on('data', chunk => {
    console.log(chunk);
  });
  response.end();
}).listen(8080);

console.log('server running on port 8080');
