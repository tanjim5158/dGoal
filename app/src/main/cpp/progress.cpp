#include "progress.h"

int calculateProgress(int completed, int total) {

    if (total <= 0) {
        return 0;
    }

    return (completed * 100) / total;
}