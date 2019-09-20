if redis.call('exists',KEYS[1]) == 0
    then
        redis.call('setex',KEYS[1],unpack(ARGV))
    else
        return 0
    end