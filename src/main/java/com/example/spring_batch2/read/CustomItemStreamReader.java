package com.example.spring_batch2.read;

import com.example.spring_batch2.entity.User;
import org.springframework.batch.item.*;

import java.util.List;

public class CustomItemStreamReader implements ItemStreamReader<User> {

    private List<User> users;
    private int index = 0;
    private boolean restart = false;
    public CustomItemStreamReader(List<User> users) {
        this.users = users;
    }

    @Override
    public User read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        User user = null;

        if(index < users.size()) {
            user = users.get(index);
            index++;
        }

        if(index == 50 && !restart){
            throw new RuntimeException("중지");
        }
        return user;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        if(executionContext.containsKey("index")){ //재 실행인 경우
            index = executionContext.getInt("index");
            this.restart = true;
        }else{ //첫 실행인 경우
            index = 0;
            executionContext.put("index", index);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.put("index", index);
    }

    @Override
    public void close() throws ItemStreamException {
    }
}
