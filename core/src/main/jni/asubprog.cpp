#include "subprog.h"

//cgi関連は不要なのでunix/usubprog.cppはリンクしない
Subprogram::Subprogram(const std::string &name, bool receiveData, bool feedData) : m_pid(-1) {}

Subprogram::~Subprogram() = default;

bool Subprogram::start(std::initializer_list<std::string> arguments, Environment &env) {
    return false;
}

bool Subprogram::wait(int *status) { return false; }

bool Subprogram::isAlive() { return false; }

void Subprogram::terminate() {
    m_pid = -1;
}
