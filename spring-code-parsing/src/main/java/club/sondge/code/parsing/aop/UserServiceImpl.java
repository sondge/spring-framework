package club.sondge.code.parsing.aop;

import club.sondge.code.parsing.definelabel.User;

public class UserServiceImpl implements UserService{
	@Override
	public void save(User user) {
		System.out.println("save User");
	}

	@Override
	public void update(User user) {
		System.out.println("update user");
	}
}
