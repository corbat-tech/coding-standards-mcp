import { Router } from 'express';
import { authController } from '../controllers/authController';
import { registerValidation, loginValidation } from '../validators/authValidators';

const router = Router();

router.post('/register', registerValidation, (req, res) => authController.register(req, res));
router.post('/login', loginValidation, (req, res) => authController.login(req, res));

export default router;
