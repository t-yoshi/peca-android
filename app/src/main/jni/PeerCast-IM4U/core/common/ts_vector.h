/*
 * Simple vector (Thread safe implementation)
 *
 *                               Impl. by Eru
 */

#ifndef _CORELIB_COMMON_TS_VECTOR_H_
#define _CORELIB_COMMON_TS_VECTOR_H_

//#include <stdexcept>
#include <cstdlib>
#include <cstring>

// Interface
template<class T> class ITSVector {
protected:
	size_t capacity;
	T **ary;

	ITSVector() :
			count(0), capacity(32) {
		ary = new T*[capacity];
	}

	size_t count;

public:

	size_t getCount() {
		return count;
	}

	virtual ~ITSVector() {
		for (size_t i = 0; i < count; ++i)
			delete ary[i];
		delete[] ary;
	}

	virtual bool empty() {
		return count == 0;
	}

	virtual void lock() = 0;
	virtual void unlock() = 0;

	virtual bool erase(size_t idx) {
		delete ary[idx];
//		for (size_t i=idx+1; i<count; ++i)
//			ary[i-1] = ary[i];
		memmove(&ary[idx], &ary[idx + 1], sizeof(T*) * (--count - idx));

		return true;
	}

	virtual bool clear() {
		for (size_t i = 0; i < count; ++i) {
			delete ary[i];
		}
		count = 0;

		return true;
	}

	virtual bool find(const T& tgt) {
		for (size_t i = 0; i < count; ++i) {
			if (*ary[i] == tgt)
				return true;
		}

		return false;
	}

	virtual bool find(const T& tgt, size_t* idx) {
		for (size_t i = 0; i < count; ++i) {
			if (*ary[i] == tgt) {
				*idx = i;
				return true;
			}
		}

		return false;
	}

	virtual bool push_back(const T& val) {
		T *ptr = new T(val);

		if (count + 1 > capacity) {
			T **ptr = new T*[capacity * 2];
			if (!ptr)
				return false;

			for (size_t i = 0; i < capacity; ++i)
				ptr[i] = ary[i];
			delete[] ary;

			ary = ptr;
			capacity <<= 1;
		}

		ary[count++] = ptr;

		return true;
	}

	virtual T& at(size_t idx) {
		if (idx >= count){
			//throw std::out_of_range("out of bounds");
			::fprintf(stderr, "[%s] out of bounds", __func__);
			::abort();//__no_return__
		}
		return *ary[idx];
	}

	virtual T& operator[](size_t idx) {
		return at(idx);
	}
};

#if _UNIX

#include <pthread.h>

template <class T>
class PTSVector : public ITSVector<T>
{
private:
	pthread_mutex_t mMutex;

public:
	PTSVector()
	{
		::pthread_mutex_init(&mMutex, NULL);
	}

	virtual ~PTSVector()
	{
		::pthread_mutex_destroy(&mMutex);
	}

	inline virtual void lock()
	{
		::pthread_mutex_lock(&mMutex);
	}

	inline virtual void unlock()
	{
		::pthread_mutex_unlock(&mMutex);
	}
};

#endif //_UNIX
#ifdef _WIN32
#include <stdlib.h>

template <class T>
class WTSVector : public ITSVector<T>
{
private:
	CRITICAL_SECTION csec;

public:
	WTSVector()
	{
		InitializeCriticalSection(&csec);
	}

	virtual ~WTSVector()
	{
		DeleteCriticalSection(&csec);
	}

	inline virtual void lock()
	{
		EnterCriticalSection(&csec);
	}

	inline virtual void unlock()
	{
		LeaveCriticalSection(&csec);
	}
};
#endif //_WIN32

#endif
