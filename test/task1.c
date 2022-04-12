#include "syscall.h"
#include "stdio.h" //This does not work for some reason

void creatTest() {
  // test whether creat successfully creates a file
  char* fileName = "creatTest.txt\0";
  int success = creat(fileName);
  if(success != -1) {
    printf("creat successfully created\n");
  }
  else {
    printf("creat unsuccessfully created\n");
    return;
  }

  // test whether creat returns -1 if the OpenFile list is full. should not be reached.
  int i;
  for(i = 0; i < 14 - 1; i++) {
    if(creat(fileName) == -1) {
      printf("creat fails as index is full. Index = %d\n", i);
      return;
    }
  }
  success = creat(fileName);
  if(success == -1) {
    printf("creat returns -1\n");
  }
  else {
    printf("creat does not return -1\n");
    return;
  }

  printf("Creat Test complete!\n\n");
}

void openTest() {
  // test whether creat successfully creates a file
  char* fileName = "creatTest2.txt\0";
  int success = creat(fileName);
  if(success != -1) {
    printf("creat successfully created\n");
  }
  else {
    printf("creat unsuccessfully created\n");
    return;
  }

  // test whether open successfully opens the created file
  success = open(fileName);
  if(success != -1) {
    printf("open successfully opens\n");
  }
  else {
    printf("open unsuccessfully opens\n");
    return;
  }

  // test whether open unsuccessfully opens nonexistent file
  fileName = "unableToOpenTest.txt\0";
  success = open(fileName);
  if(success == -1) {
    printf("open successfully fails\n");
  }
  else {
    printf("open unsuccessfully fails\n");
    return;
  }

  // test whether open returns -1 if the OpenFile list is full
  fileName = "creatTest3.txt\0";
  
  success = open(fileName);
  if(success == -1) {
    printf("open returns -1\n");
  }
  else {
    printf("open does not return -1\n");
    return;
  }

  printf("Open Test complete!\n\n");
}

void readWriteTest() {
  // test whether open successfully opens the created file
  char* readFileName = "TestRead.txt\0";
  char* writeFileName = "TestWrite.txt\0";
  int readFD = open(readFileName);
  int writeFD = open(writeFileName);
  if(readFD != -1 && writeFD) {
    printf("open successfully opens\n");
  }
  else {
    printf("open unsuccessfully opens\n");
    return;
  }

  // test whether read successfully reads a file and writes to the disk
  void* buffer;
  int count = 50;
  int transferred;
  do {
    transferred = read(readFD, buffer, count);
  } while(write(writeFD, buffer, transferred) > 0);
  printf("Finished reading and writing! Check out %s and %s for sanity check\n", readFileName, writeFileName);

  // test whether stdout successfully prints out from the read file
  readFD = open(readFileName);
  writeFD = 1;  // stdout
  if(readFD != -1 && writeFD) {
    printf("open successfully opens\n");
  }
  else {
    printf("open unsuccessfully opens\n");
    return;
  }
  do {
    transferred = read(readFD, buffer, count);
  } while(write(writeFD, buffer, transferred) > 0);
  printf("Finished reading and writing! Check out %s and output\n", readFileName);

  printf("Read & Write Test complete!\n");
}

void closeUnlinkTest() {
  // test whether creat successfully creates a file
  char* fileName1 = "removeTest1.txt\0";
  char* fileName2 = "removeTest2.txt\0";
  int file1 = creat(fileName1);
  int file2 = creat(fileName2);
  int file3 = creat(fileName2);
  if(file1 != -1) {
    printf("creat successfully creates\n");
  }
  else {
    printf("creat unsuccessfully creates\n");
    return;
  }

  // test whether close successfully removes OpenFile from the list
  close(file1);
  void* buffer;
  int count = 1;
  int transferred = read(file1, buffer, count);
  if(transferred == -1 && write(file1, buffer, transferred) == -1) {
    printf("close successfully removed\n");
  }
  else {
    printf("close unsuccessfully removed\n");
    return;
  }

  // test whether whether unlink successfully removes the file
  if(unlink(fileName1) != -1) {
    printf("unlink successfully removed the following file: %s!\n", fileName1);
  }
  else {
    printf("unlink unsuccessfully removed the following file: %s!\n", fileName1);
    return;
  }

  if(unlink(fileName2) == -1) {
    printf("unlink successfully failed to remove the following file: %s!\n", fileName2);
  }
  else {
    printf("unlink unsuccessfully failed to remove the following file: %s!\n", fileName2);
    return;
  }
  if(creat(fileName2) == -1 && open(fileName2) == -1) {
    printf("creat and open successfully failed to work\n");
  }
  else {
    printf("creat and open unsuccessfully failed to work\n");
    return;
  }
  if(close(file2) == -1 && close(file3) == 0) {
    printf("close successfully removed %s from the disk!\n", fileName2);
  }
  else {
    printf("close unsuccessfully removed %s from the disk\n!", fileName2);
    return;
  }
  if(creat(fileName2) != -1 && open(fileName2) != -1) {
    printf("creat and open successfully work again for the file: %s\n", fileName2);
  }
  else {
    printf("creat and open unsuccessfully work again for the file: %s\n", fileName2);
    return;
  }

  printf("Close & Unlink Test complete!\n\n");
}


int main(int argc, char** argv) {
      
/*
*Change input for test cases.
1. Create
2. Open
3. Read/Write
4. Close/Unlink
*/
int input = 3; //If stdio.h worked this would be a scanf but the mips path is messing with it

if(input == 1){	
	printf("Create:\n");
      creatTest();
}
if(input == 2){
      printf("Open:\n");
      openTest();
}
if(input == 3){
      printf("Read/Write: \n");
      readWriteTest();
}
if(input == 4){
      printf("Close/Unlink: \n");
      closeUnlinkTest();
 }

  halt();
}

