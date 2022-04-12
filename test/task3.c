#include "syscall.h"
#include "stdio.h"

#define MAXOPENFILES 14

// menu to choose which test to run
enum TESTOPTION {EXEC, JOIN, EXIT};

//int exec(char *file, int argc, char *argv[]);
void execTest()
{
  char* fileName = "task3exec.coff\0";
  int argc = 1;
  char* argv[]= {"2"};
		
int pid = exec(fileName, argc , argv );
printf("%d \n", pid);
	if(pid != -1) 
	{
    		printf("process ID exists!\n");
	}
  	else 
	{
    		printf("processID does not exist!\n");
    		return;
	}

}

void execTest2()
{
  char* fileName = "echo.coff\0";
  int argc = 1;
  char* argv[]= {"2"};
		
int pid = exec(fileName, argc , argv );
printf("%d \n", pid);
	if(pid != -1) 
	{
    		printf("process ID exists!\n");
		joinTest(pid);
	}
  	else 
	{
    		printf("processID does not exist!\n");
    		return;
	}

}



//int join(int processID, int *status);
void joinTest()
{
char* fileName = "echo.coff\0";
  int argc = 1;
  char* argv[]= {"2"};
		
int pid = exec(fileName, argc , argv );
int status;
printf("%d \n", pid);
	if(pid != -1) 
	{
    		printf("process ID exists!\n");
		int valid = join(pid, &status);
		
		if(valid != -1)
			printf("join success!\n");
	}
  	else 
	{
    		printf("join failure!\n");
    		return;
	}


}


//void exit(int status);
void exitTest()
{
 char* fileName = "echo.coff\0";
  int argc = 1;
  char* argv[argc];
  argv[0] = fileName;
		
 exit(1);

}

int main() 
{
 
      printf("Exec: \n");
      execTest();
     
      printf("Join:\n");
      joinTest();
   
      printf("Exit: \n");
      exitTest();
      

  halt(); // "exit" function for task3
}
