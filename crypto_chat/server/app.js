// Setup basic express server
var express = require('express');
var app = express();
var server = require('http').createServer(app);

var io = require('socket.io')(server,{path:'/socket.io'});
var mysql = require('mysql');
var port = process.env.PORT || 8080;

var connection = mysql.createConnection({
    host: 'localhost',
    query: {
        pool: true
    },
    user: 'root',
    password: '201520927q',
    database: 'security'

});

// Routing
app.use(express.static(__dirname + '/public'));

// register
app.get('/register/', function(req,res){
  var sql = 'INSERT INTO login VALUES("'+'","' + req.query.studentId + '","' + req.query.password+ '","'+req.query.name+'")'
  connection.query(sql, function(err, rows, fields) {
    if(err){
	res.sendStatus(400);
	console.log("RegisterStatus : Err!");
	return;
    }
    else{
  	res.sendStatus(201);
	console.log("RegisterStatus : Success!");
	return;
    }

  });

});
app.get('/insertMessage/',function(req,res){
  var tmp = "('','"+req.query.message+"','"+req.query.messagefrom+"')"
  while(tmp.search(' ')!= -1){
    tmp.replace(' ','+');
  }

  var sql = 'INSERT INTO Messages VALUES'+tmp;
  connection.query(sql, function(err, rows, fields) {
     if(err){
	res.sendStatus(400);
	console.log("InsertMessage : Err!");
	return;
    }
    else{
  	res.sendStatus(201);
	console.log("InsertMessage : Success!!!!!!!!!!!!!!!!!!!!!!");
	return;
    }
  });
});
app.get('/getMessages/' , function(req, res) {
    var sql = 'SELECT * FROM Messages';
    connection.query(sql,function(err,rows,fields){
      if(err){
 	res.sendStatus(400);
	console.log("GetMessages : Err!");
	return;
      }
      if(rows.length ==0){
	res.sendStatus(400);
	console.log("GetMessages : Null!");
      } else{
	console.log(rows);
        res.status(201).send(rows);
	console.log("GetMessages : Success!");
	res.end();
    }
  });
});

// Login
app.get('/loginCheck/', function(req, res) {

    var sql = 'SELECT Name FROM login WHERE StudentId = "' + req.query.studentId + '" AND Password = "' + req.query.password + '"';
    connection.query(sql, function(err, rows, fields) {
        if (err) {
            res.sendStatus(400);
            console.log("LoginStatus : Err!");
            return;
        }
        if (rows.length == 0) {
            res.sendStatus(204);
            console.log("LoginStatus : login Failed");
        } else {
            console.log(rows);
            res.status(201).send(rows);
            console.log("LoginStatus : login OK!");
            res.end();
        }
    });
});


// Chat

var numUsers = 0;

server.listen(port, function() {
    console.log('Server listening at port %d', port);
});


io.on("connection",function(socket) {
    var addedUser = false;
    console.log('socket connected');
    socket.on('new message', function(data) {
        socket.broadcast.emit('new message', {
            username: socket.username,
            message: data
        });
        console.log('user: ' + socket.username + ' | message: ' + data);
    });


    socket.on('add user', function(username) {
        if (addedUser) return;

        socket.username = username;
        ++numUsers;
        addedUser = true;
        socket.emit('login', {
            numUsers: numUsers
        });

        socket.broadcast.emit('user joined', {
            username: socket.username,
            numUsers: numUsers
        });
        console.log('numUsers: ' + numUsers);
    });


    socket.on('typing', function() {
        socket.broadcast.emit('typing', {
            username: socket.username
        });
    });


    socket.on('stop typing', function() {
        socket.broadcast.emit('stop typing', {
            username: socket.username
        });
    });


    socket.on('disconnect', function() {
        if (addedUser) {
            --numUsers;

            socket.broadcast.emit('user left', {
                username: socket.username,
	     });
        }
     });
});