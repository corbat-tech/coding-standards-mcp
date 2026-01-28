import { Router } from 'express';
import { userController } from '../controllers/userController';
import { authMiddleware } from '../middleware/authMiddleware';
import { updateProfileValidation } from '../validators/authValidators';

const router = Router();

router.get('/me', authMiddleware, (req, res) => userController.getProfile(req, res));
router.put('/me', authMiddleware, updateProfileValidation, (req, res) => userController.updateProfile(req, res));

export default router;
