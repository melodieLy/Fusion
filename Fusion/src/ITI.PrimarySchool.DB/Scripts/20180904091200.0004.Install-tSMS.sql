create table iti.tSMS
(
  SMSId int identity(0, 1),
  DevicesId INT NOT NULL,
  UsersId INT NOT NULL,
  Extern VARCHAR(45) NOT NULL,
  [Time] DATETIME NOT NULL,
  [Message] TEXT,
  direction BIT NOT NULL Default 0,

  constraint PK_tSMS primary key(SMSId),
  constraint FK_tSMS_tUsers foreign key(UsersId) references iti.tUsers(UsersId),
  constraint FK_tSMS_tDevices foreign key(DevicesId) references iti.tDevices(DevicesId)
)