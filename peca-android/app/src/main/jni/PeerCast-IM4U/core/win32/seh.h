#ifndef _SEH_H_
#define _SEH_H_

#include "stream.h"
#include <dbghelp.h>


extern FileStream fs;

#if defined (_MSC_VER) && !defined(_DEBUG)

#pragma once
#pragma comment(lib, "dbghelp.lib")
#define SEH_THREAD(func, name) \
{ \
	__try \
	{ \
		return func(thread); \
	} __except(SEHdump(GetExceptionInformation()), EXCEPTION_EXECUTE_HANDLER) \
	{ \
	} \
}

#else

#define SEH_THREAD(func, name) return func(thread);
#endif

void SEHdump(_EXCEPTION_POINTERS *);

#endif
